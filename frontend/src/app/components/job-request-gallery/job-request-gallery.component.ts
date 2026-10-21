import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { JobRequestService } from '../../services/job-request.service';
import { JobOfferService } from '../../services/job-offer.service';
import { LicenseService } from '../../services/license.service';
import { AuthService } from '../../services/auth.service';
import { ChatService } from '../../services/chat.service';
import { PropertyService } from '../../services/property.service';
import { ReportService } from '../../services/report.service';

import { JobRequestImage, JobRequestPrice, JobRequestSearch } from '../../dtos/jobRequest';
import { JobOfferWithWorker } from '../../dtos/jobOffer';
import { Property } from '../../dtos/property';
import { ReportCreate, ReportType } from '../../dtos/report';

import { JobRequestDatacardComponent, JobCardData } from '../job-request-datacard/job-request-datacard.component';
import { FilterSidebarComponent } from '../filter-sidebar/filter-sidebar.component';
import { ConfirmDeleteDialogComponent } from '../confirmation-dialogues/confirm-delete-dialog/confirm-delete-dialog.component';
import { ReportDialogComponent } from '../report-dialog/report-dialog.component';
import {Page} from "../../dtos/page";
import {License} from "../../dtos/license";
import {Paginator, PaginatorState} from "primeng/paginator";

declare var bootstrap: any;

@Component({
  selector: 'app-job-request-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    JobRequestDatacardComponent, FilterSidebarComponent, ConfirmDeleteDialogComponent, ReportDialogComponent, Paginator
  ],
  templateUrl: './job-request-gallery.component.html',
  styleUrls: ['./job-request-gallery.component.scss'],
})
export class JobRequestGalleryComponent implements OnInit, OnDestroy {

  // --- Component State ---
  mode: 'admin' | 'customer' | 'worker' = 'worker';
  pageSize = 10;
  jobRequestPage: Page<JobRequestPrice> = {
    content: [],
    totalElements: 0,
    pageSize: 0,
    offset: 0
  };
  loading = false;
  isFilterSidebarOpen = false;
  jobImages: Record<number, string> = {};

  // --- Mode-Specific Properties ---
  // Customer
  offersMap: Record<number, JobOfferWithWorker[]> = {};
  selectedJobId: number | null = null;
  customerProperties: Property[] = [];
  // Admin
  jobForDeletion: JobRequestPrice | undefined;
  // Worker
  licenseApproved: boolean | null = null;
  jobForReport: JobRequestPrice | null = null;
  searchParametersFromRoute: JobRequestSearch = {};

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private jobRequestService: JobRequestService,
    private jobOfferService: JobOfferService,
    private licenseService: LicenseService,
    private authService: AuthService,
    private chatService: ChatService,
    private propertyService: PropertyService,
    private reportService: ReportService,
    private notification: ToastrService
  ) {}

  get hasProperties(): boolean {
    return this.customerProperties && this.customerProperties.length > 0;
  }

  ngOnInit(): void {
    this.mode = this.route.snapshot.data['mode'];

    if (this.mode === 'customer') {
      this.loadCustomerProperties();
    }
    if (this.mode === 'worker') {
      this.route.queryParams.subscribe(params => {
        this.searchParametersFromRoute = {
          title: params['keywords'] || '',
          distance: params['maxDistance'] ? +params['maxDistance'] : undefined,
          category: params['categories'] || ''
        };
      });

      this.checkWorkerLicense(this.searchParametersFromRoute);
    } else {
      this.reloadJobRequests({});
    }
  }

  ngOnDestroy(): void {
    // Revoke all created blob URLs to prevent memory leaks
    Object.values(this.jobImages).forEach(url => {
      if (url.startsWith('blob:')) {
        URL.revokeObjectURL(url);
      }
    });
  }

  // --- Data Loading ---
  reloadJobRequests(searchParameters: JobRequestSearch, offset: number = 0, limit: number = this.pageSize): void {
    if (this.mode === 'worker' && !this.licenseApproved) {
      return;
    }

    this.loading = true;
    let request$: Observable<Page<JobRequestPrice>>;

    switch (this.mode) {
      case 'admin':
        request$ = this.jobRequestService.searchJobRequestsAdmin(searchParameters, offset, limit);
        break;
      case 'customer':
        request$ = this.jobRequestService.searchJobRequestsCustomer(searchParameters, offset, limit);
        break;
      case 'worker':
        request$ = this.jobRequestService.searchJobRequestsWorker(searchParameters, offset, limit);
        break;
    }

    request$.subscribe({
      next: (data) => {
        this.jobRequestPage = {
          content: data.content,
          totalElements: data.totalElements,
          offset,
          pageSize: limit
        };
        this.loadJobRequestImages();
        if (this.mode === 'customer') {
          this.loadOffers();
        }
        this.loading = false;
        console.log(data)
      },
      error: (err) => {
        this.notification.error(err.error?.message || 'Failed to load job requests', 'Error');
        this.loading = false;
      },
    });
  }


  private loadJobRequestImages(): void {
    if (!this.jobRequestPage.content || this.jobRequestPage.content.length === 0) {
      return;
    }

    // Clean up any previously loaded image URLs
    Object.values(this.jobImages).forEach(url => {
      if (url.startsWith('blob:')) URL.revokeObjectURL(url);
    });
    this.jobImages = {};

    this.jobRequestPage.content.forEach(job => {
      if (!job.id) return;

      this.jobRequestService.getJobRequestImages(job.id).subscribe({
        next: (images: JobRequestImage[]) => {
          if (images?.length > 0) {
            // Get the image with the highest display position (or last one)
            const primaryImage = images.sort((a, b) => a.displayPosition - b.displayPosition).pop();
            if (primaryImage?.id) {
              this.jobRequestService.getImageData(job.id, primaryImage.id).subscribe({
                next: (imageData: Blob) => {
                  this.jobImages[job.id!] = URL.createObjectURL(imageData);
                },
                error: () => {
                  this.jobImages[job.id!] = 'assets/BRP_logo.png'; // Use placeholder on image data error
                }
              });
            } else {
              this.jobImages[job.id!] = 'assets/BRP_logo.png'; // Use placeholder if no valid image ID
            }
          } else {
            this.jobImages[job.id!] = 'assets/BRP_logo.png'; // Use placeholder if no images exist
          }
        },
        error: () => {
          this.jobImages[job.id!] = 'assets/BRP_logo.png'; // Use placeholder on metadata fetch error
        }
      });
    });
  }

  // --- Mode-Specific Logic ---
  checkWorkerLicense(searchParams: JobRequestSearch): void {
    const username = this.authService.getUsername();
    if (!username) {
      this.licenseApproved = false;
      return;
    }
    this.licenseService.listCertificate(username).subscribe({
      next: (licenses) => {
        // Case 1: The worker has not uploaded any licenses.
        if (!licenses || licenses.length === 0) {
          this.licenseApproved = false;
          // find the modal element and show it.
          const noLicensesModal = new bootstrap.Modal(document.getElementById('no-licenses-modal'));
          noLicensesModal.show();
          return; // stop further execution
        }

        // Case 2: The worker has licenses, check if any are approved.
        this.licenseApproved = licenses.some((l) => l.status === 'APPROVED');
        if (this.licenseApproved) {
          this.reloadJobRequests(searchParams);
        }
      },
      error: () => {
        this.licenseApproved = false;
        const noLicensesModal = new bootstrap.Modal(document.getElementById('no-licenses-modal'));
        noLicensesModal.show();
      },
    });
  }

  loadCustomerProperties(): void {
    this.propertyService.listProperties().subscribe(props => {
      this.customerProperties = props;

      if (!this.hasProperties) {
        const noPropertiesModal = new bootstrap.Modal(document.getElementById('no-properties-modal'));
        noPropertiesModal.show();
      }
    });
  }

  loadOffers(): void {
    this.jobOfferService.getOffersForCustomer().subscribe(offers => {
      this.offersMap = {};
      for (const offer of offers) {
        if (!this.offersMap[offer.jobRequestId]) {
          this.offersMap[offer.jobRequestId] = [];
        }
        this.offersMap[offer.jobRequestId].push(offer);
      }
    });
  }

  // --- Event Handlers ---
  onSearchCriteriaChanged(searchParameters: JobRequestSearch): void {
    console.log('Suche gestartet mit Kriterien:', searchParameters);
    this.searchParametersFromRoute = searchParameters;
    this.reloadJobRequests(searchParameters);
  }


  onEditJob(job: JobCardData): void {
    this.router.navigate(['/customer/requests', job.id, 'edit']);
  }

  onViewDetails(job: JobCardData): void {
    if (this.mode === 'admin') {
      this.router.navigate(['/admin/requests', job.id, 'details']);
    } else if (this.mode === 'worker') {
      this.router.navigate(['/worker/requests', job.id, 'details']);
    } else {
      this.router.navigate(['/customer/requests', job.id, 'details']);
    }
  }

  onCreateOffer(job: JobCardData): void {
    this.router.navigate(['/worker/requests', job.id, 'offers', 'create']);
  }

  onDeleteJob(job: JobCardData): void {
    this.jobForDeletion = job;
    const deleteModal = new bootstrap.Modal(document.getElementById('delete-dialog'));
    deleteModal.show();
  }

  deleteJob(job: JobRequestPrice | undefined): void {
    if (!job?.id) return;
    this.jobRequestService.deleteRequest(job.id).subscribe({
      next: () => {
        this.notification.success(`Job "${job.title}" deleted.`);
        this.reloadJobRequests({});
      },
      error: (err) => this.notification.error(err.error?.message || 'Failed to delete job.', 'Error')
    });
  }

  toggleJobOffers(jobId: number): void {
    if (this.mode !== 'customer') return;
    this.selectedJobId = this.selectedJobId === jobId ? null : jobId;
  }

  toggleFilterSidebar(): void {
    this.isFilterSidebarOpen = !this.isFilterSidebarOpen;
  }

  acceptOffer(offer: JobOfferWithWorker): void {
    this.jobOfferService.acceptOffer(offer).subscribe({
      next: () => {
        this.notification.success('Offer accepted!');
        this.reloadJobRequests({});
      },
      error: err => {
        this.notification.error(err.error?.message || 'Offer could not be accepted.', 'Error');
        this.reloadJobRequests({});
      }
    });
  }

  requestDone(requestId: number): void {
    this.jobRequestService.requestDone(requestId).subscribe({
      next: () => {
        this.notification.success('Job marked as Done!');
        this.reloadJobRequests({});
      },
      error: err => {
        this.notification.error(err.error?.message || 'Failed to mark job as done.', 'Error');
      }
    });
  }

  openChat(jobId: number): void {
    this.chatService.engageConversation(jobId).subscribe({
      next: createdChat => {
        this.router.navigate(['/chats', createdChat.chatId]);
      },
      error: err => {
        this.notification.error(err.error?.message || 'Could not open chat.', 'Error');
      },
    });
  }

  onReport(job: JobCardData): void {
    this.jobForReport = job;
    const reportModal = new bootstrap.Modal(document.getElementById('report-dialog'));
    reportModal.show();
  }

  onReportConfirm(reason: string): void {
    if (!this.jobForReport?.id) return;
    const payload: ReportCreate = {
      jobRequestId: this.jobForReport.id,
      type: ReportType.JOB_REQUEST,
      reason: reason,
    };
    this.reportService.create(payload).subscribe({
      next: () => this.notification.success('Report submitted successfully.'),
      error: (err) => this.notification.error(err.error?.message || 'Failed to submit report.', 'Error'),
    });
  }

  // --- Helper Methods ---
  enhancedJobData(job: JobRequestPrice): JobCardData {
    return {
      ...job,
      deadlineLocale: job.deadline ? new Date(job.deadline).toLocaleDateString() : 'N/A',
      categoryDisplayName: job.category?.replace(/_/g, ' '),
      imageUrl: this.jobImages[job.id!]
    };
  }

  onPageChange(event: PaginatorState): void {
    const newOffset = event.first ?? 0;
    const newLimit = event.rows ?? this.pageSize;

    this.jobRequestPage.offset = newOffset;
    this.jobRequestPage.pageSize = newLimit;

    const searchParams = this.searchParametersFromRoute;

    this.reloadJobRequests(searchParams, newOffset, newLimit);
  }

}
