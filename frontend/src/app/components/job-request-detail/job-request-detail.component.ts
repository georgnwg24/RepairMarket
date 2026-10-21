import { Component, OnInit, AfterViewInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule, formatDate, Location } from '@angular/common';
import { ToastrService } from "ngx-toastr";
import { GalleriaModule, Galleria } from 'primeng/galleria';

import { JobRequestService } from '../../services/job-request.service';
import { PropertyService } from '../../services/property.service';
import { ReportService } from '../../services/report.service';
import { AuthService } from '../../services/auth.service';

import { JobRequest, JobRequestImage, JobRequestWithUser } from '../../dtos/jobRequest';
import { Property } from "../../dtos/property";
import { ReportCreate, ReportType } from '../../dtos/report';

import { ReportDialogComponent } from '../report-dialog/report-dialog.component';
import {
  ConfirmDeleteDialogComponent
} from "../confirmation-dialogues/confirm-delete-dialog/confirm-delete-dialog.component";

declare var bootstrap: any;

export interface GalleryItem {
  id: number;
  src: string;
  thumbnail: string;
  alt: string;
  title: string;
}

@Component({
  selector: 'app-job-request-detail',
  templateUrl: './job-request-detail.component.html',
  styleUrls: ['./job-request-detail.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    GalleriaModule,
    ReportDialogComponent,
    RouterLink,
    ConfirmDeleteDialogComponent,
  ]
})
export class JobRequestDetailComponent implements OnInit, AfterViewInit {
  @ViewChild('galleria') galleria: Galleria;

  jobRequest: JobRequestWithUser | undefined;
  property: Property | null = null;
  existingImages: JobRequestImage[] = [];

  propertyLoading = false;
  imagesLoaded = false;
  origin = 'requests';
  mode: string;

  private imageCache: { [key: number]: string } = {};
  private thumbnailCache: { [key: number]: string } = {};

  galleryItems: GalleryItem[] = [];
  activeIndex: number = 0;
  responsiveOptions = [
    {
      breakpoint: '1024px',
      numVisible: 5
    },
    {
      breakpoint: '768px',
      numVisible: 3
    },
    {
      breakpoint: '560px',
      numVisible: 2
    }
  ];

  jobForReport: JobRequestWithUser | null = null;
  lowestOfferPrice: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private jobRequestService: JobRequestService,
    private propertyService: PropertyService,
    private notification: ToastrService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private location: Location,
    private authService: AuthService,
    private reportService: ReportService,
  ) {
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.origin = params['from'] === 'offers' ? 'offers' : 'requests';
    });

    this.mode = this.authService.getUserRole();

    const id = Number(this.route.snapshot.paramMap.get('requestId'));
    if (id) {
      this.loadJobRequestDetails(id);
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.refreshGalleria(), 100);
  }

  private loadJobRequestDetails(id: number): void {
    this.jobRequestService.getByIdWithCustomer(id).subscribe({
      next: data => {
        this.jobRequest = data;
        if (data.propertyId) {
          this.loadPropertyDetails(data.propertyId);
        }
        this.loadAndProcessImages(id);
      },
      error: error => {
        this.handleError('Error loading job request:', error, 'Job request no longer exists.');
        this.location.back();
      }
    });
  }

  private loadPropertyDetails(propertyId: number): void {
    this.propertyLoading = true;
    this.propertyService.getProperty(propertyId).subscribe({
      next: property => {
        this.property = property;
        this.propertyLoading = false;
      },
      error: error => {
        this.handleError('Error loading property:', error);
        this.property = null;
        this.propertyLoading = false;
      }
    });
  }

  private loadAndProcessImages(jobId: number): void {
    this.jobRequestService.getImages(jobId).subscribe({
      next: images => {
        this.existingImages = images;
        this.imagesLoaded = true;
        if (images.length > 0) {
          const imageProcessingPromises = images.map(image => this.loadImageAndGenerateThumbnail(jobId, image.id));

          Promise.all(imageProcessingPromises).then(() => {
            this.updateGalleryItems();
            this.refreshGalleria();
          }).catch(err => {
            this.handleError('Could not process all images.', err);
            this.updateGalleryItems();
          });
        }
      },
      error: err => {
        this.handleError('Failed to load image metadata.', err);
        this.imagesLoaded = true;
      }
    });
  }

  private loadImageAndGenerateThumbnail(jobId: number, imageId: number): Promise<void> {
    return new Promise((resolve) => {
      if (this.imageCache[imageId] && this.thumbnailCache[imageId]) {
        return resolve();
      }

      this.jobRequestService.getImageData(jobId, imageId).subscribe({
        next: (blob) => {
          const reader = new FileReader();
          reader.onload = async (e: any) => {
            const fullImageUrl = e.target.result as string;
            this.imageCache[imageId] = fullImageUrl;

            try {
              const thumbnailBlob = await this.createThumbnailFromBlob(blob);
              const thumbReader = new FileReader();
              thumbReader.onload = () => {
                this.thumbnailCache[imageId] = thumbReader.result as string;
                resolve();
              };
              thumbReader.readAsDataURL(thumbnailBlob);
            } catch (err) {
              console.warn(`Failed to create thumbnail for image ${imageId}, using full image.`, err);
              this.thumbnailCache[imageId] = fullImageUrl;
              resolve();
            }
          };
          reader.readAsDataURL(blob);
        },
        error: (err) => {
          console.error(`Error loading image data for ID ${imageId}:`, err);
          this.imageCache[imageId] = 'assets/BRP_logo.png'; // Fallback to placeholder
          this.thumbnailCache[imageId] = 'assets/BRP_logo.png';
          resolve();
        }
      });
    });
  }

  private createThumbnailFromBlob(blob: Blob): Promise<Blob> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.src = URL.createObjectURL(blob);
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        const MAX_SIZE = 150;

        let { width, height } = img;
        if (width > height) {
          if (width > MAX_SIZE) {
            height *= MAX_SIZE / width;
            width = MAX_SIZE;
          }
        } else {
          if (height > MAX_SIZE) {
            width *= MAX_SIZE / height;
            height = MAX_SIZE;
          }
        }

        canvas.width = width;
        canvas.height = height;
        ctx.drawImage(img, 0, 0, width, height);

        canvas.toBlob(thumbnailBlob => {
          URL.revokeObjectURL(img.src);
          if (thumbnailBlob) {
            resolve(thumbnailBlob);
          } else {
            reject(new Error('Canvas to Blob conversion failed.'));
          }
        }, 'image/jpeg', 0.8);
      };
      img.onerror = (err) => {
        URL.revokeObjectURL(img.src);
        reject(err);
      };
    });
  }

  private updateGalleryItems(): void {
    const newItems: GalleryItem[] = [];
    this.existingImages.forEach(image => {
      if (this.imageCache[image.id]) {
        newItems.push({
          id: image.id,
          src: this.imageCache[image.id],
          thumbnail: this.thumbnailCache[image.id] || this.imageCache[image.id],
          alt: `Job image ${image.id}`,
          title: `Image ${image.id}`
        });
      }
    });
    this.galleryItems = newItems;
    this.cdr.detectChanges();
  }

  private refreshGalleria(): void {
    if (!this.galleria) return;

    const tempItems = [...this.galleryItems];
    this.galleryItems = [];
    this.cdr.detectChanges();

    setTimeout(() => {
      this.galleryItems = tempItems;
      this.cdr.detectChanges();
    }, 50);
  }

  formatDisplayString(input: string): string {
    if (!input) {
      return '';
    }
    return input.charAt(0).toUpperCase() + input.slice(1).toLowerCase();
  }

  formatDate(dateString: string): string {
    return formatDate(dateString, 'mediumDate', 'en-US', '+0200');
  }

  private handleError(message: string, error: any, toastMessage?: string): void {
    console.error(message, error);
    if (toastMessage) {
      this.notification.error(toastMessage, 'Error');
    }
  }

  goBack(): void {
    this.location.back();
  }

  onCreateOfferClick(): void {
    if (this.jobRequest) {
      this.router.navigate(['/worker', 'requests', this.jobRequest.id, 'offers', 'create']);
    }
  }

  onReportClick(): void {
    this.jobForReport = this.jobRequest;
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

  onEditClick(): void {
    if (this.jobRequest) {
      this.router.navigate(['/customer', 'requests', this.jobRequest.id, 'edit']);
    }
  }

  onDeleteClick(): void {
    const deleteModal = new bootstrap.Modal(document.getElementById('delete-dialog'));
    deleteModal.show();
  }

  deleteJob(job: JobRequest | undefined): void {
    if (!job?.id) return;
    this.jobRequestService.deleteRequest(job.id).subscribe({
      next: () => {
        this.notification.success(`Job "${job.title}" deleted.`);
        this.router.navigate(['/customer', 'requests']);
      },
      error: (err) => this.notification.error(err.error?.message || 'Failed to delete job.', 'Error')
    });
  }

  countryMap: { [code: string]: string } = {
    'AD': 'Andorra', 'AE': 'United Arab Emirates', 'AI': 'Anguilla', 'AL': 'Albania', 'AR': 'Argentina',
    'AS': 'American Samoa', 'AT': 'Austria', 'AU': 'Australia', 'AX': 'Åland Islands', 'AZ': 'Azerbaijan',
    'BD': 'Bangladesh', 'BE': 'Belgium', 'BG': 'Bulgaria', 'BM': 'Bermuda', 'BR': 'Brazil', 'BY': 'Belarus',
    'CA': 'Canada', 'CC': 'Cocos (Keeling) Islands', 'CH': 'Switzerland', 'CL': 'Chile', 'CN': 'China',
    'CO': 'Colombia', 'CR': 'Costa Rica', 'CX': 'Christmas Island', 'CY': 'Cyprus', 'CZ': 'Czechia',
    'DE': 'Germany', 'DK': 'Denmark', 'DO': 'Dominican Republic', 'DZ': 'Algeria', 'EC': 'Ecuador',
    'EE': 'Estonia', 'ES': 'Spain', 'FI': 'Finland', 'FK': 'Falkland Islands', 'FM': 'Micronesia',
    'FO': 'Faroe Islands', 'FR': 'France', 'GB': 'United Kingdom', 'GF': 'French Guiana', 'GG': 'Guernsey',
    'GI': 'Gibraltar', 'GL': 'Greenland', 'GP': 'Guadeloupe', 'GS': 'South Georgia & South Sandwich Islands',
    'GT': 'Guatemala', 'GU': 'Guam', 'HK': 'Hong Kong SAR China', 'HM': 'Heard & McDonald Islands', 'HN': 'Honduras',
    'HR': 'Croatia', 'HT': 'Haiti', 'HU': 'Hungary', 'ID': 'Indonesia', 'IE': 'Ireland', 'IM': 'Isle of Man',
    'IN': 'India', 'IO': 'British Indian Ocean Territory', 'IS': 'Iceland', 'IT': 'Italy', 'JE': 'Jersey',
    'JP': 'Japan', 'KE': 'Kenya', 'KR': 'South Korea', 'LI': 'Liechtenstein', 'LK': 'Sri Lanka', 'LT': 'Lithuania',
    'LU': 'Luxembourg', 'LV': 'Latvia', 'MA': 'Morocco', 'MC': 'Monaco', 'MD': 'Moldova', 'MH': 'Marshall Islands',
    'MK': 'North Macedonia', 'MO': 'Macao SAR China', 'MP': 'Northern Mariana Islands', 'MQ': 'Martinique',
    'MT': 'Malta', 'MW': 'Malawi', 'MX': 'Mexico', 'MY': 'Malaysia', 'NC': 'New Caledonia', 'NF': 'Norfolk Island',
    'NL': 'Netherlands', 'NO': 'Norway', 'NR': 'Nauru', 'NU': 'Niue', 'NZ': 'New Zealand', 'PA': 'Panama', 'PE': 'Peru',
    'PF': 'French Polynesia', 'PH': 'Philippines', 'PK': 'Pakistan', 'PL': 'Poland', 'PM': 'St. Pierre & Miquelon',
    'PN': 'Pitcairn Islands', 'PR': 'Puerto Rico', 'PT': 'Portugal', 'PW': 'Palau', 'RE': 'Réunion',
    'RO': 'Romania', 'RS': 'Serbia', 'RU': 'Russia', 'SE': 'Sweden', 'SG': 'Singapore', 'SI': 'Slovenia',
    'SJ': 'Svalbard & Jan Mayen', 'SK': 'Slovakia', 'SM': 'San Marino', 'TC': 'Turks & Caicos Islands',
    'TH': 'Thailand', 'TR': 'Turkey', 'UA': 'Ukraine', 'US': 'United States', 'UY': 'Uruguay',
    'VA': 'Vatican City', 'VI': 'U.S. Virgin Islands', 'WF': 'Wallis & Futuna', 'WS': 'Samoa',
    'YT': 'Mayotte', 'ZA': 'South Africa', 'OTH': 'Other'
  };

  /**
   * Translates a country code to its full name using the component's countryMap.
   * @param code The two-letter country code (e.g., 'AT').
   * @returns The full country name, or the code itself if not found in the map.
   */
  public getCountryName(code: string): string {
    return this.countryMap[code] || code;
  }
}
