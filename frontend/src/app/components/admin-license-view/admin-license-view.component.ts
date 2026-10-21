import { Component, OnInit } from '@angular/core';
import { License } from '../../dtos/license';
import { LicenseService } from '../../services/license.service';
import {DatePipe, NgForOf, NgIf} from "@angular/common";
import {saveAs} from "file-saver";
import {ToastrService} from "ngx-toastr";
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import {forkJoin, Observable} from 'rxjs';
import {Page} from "../../dtos/page";
import {ReportListDto} from "../../dtos/report";
import {Paginator} from "primeng/paginator";

@Component({
  selector: 'app-admin-license-view',
  templateUrl: './admin-license-view.component.html',
  imports: [
    DatePipe,
    NgIf,
    NgForOf,
    ReactiveFormsModule,
    Paginator
  ],
  styleUrls: ['./admin-license-view.component.scss']
})
export class AdminLicenseViewComponent implements OnInit {
  selectedTab: 'pending' | 'approved' | 'rejected' = 'pending';
  pageSize = 10;

  currentOffsets = {
    pending: 0,
    approved: 0,
    rejected: 0
  };
  pendingLicenses: Page<License> = {
    content: [],
    totalElements: 0,
    pageSize: 0,
    offset: 0
  };
  approvedLicenses: Page<License> = {
    content: [],
    totalElements: 0,
    pageSize: 0,
    offset: 0
  };
  rejectedLicenses: Page<License> = {
    content: [],
    totalElements: 0,
    pageSize: 0,
    offset: 0
  };

  loading = false;
  error = '';
  searchControl = new FormControl('');
  private searchTerm = '';

  constructor(
    private licenseService: LicenseService,
    private notification: ToastrService
  ) {}

  ngOnInit(): void {
    this.loadCurrentLicenses();

    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(val => {
        this.searchTerm = (val || '').trim().toLowerCase();
        this.loadCurrentLicenses(this.currentOffsets[this.selectedTab]);
      });
  }

  selectTab(tab: 'pending' | 'approved' | 'rejected'): void {
    if (this.selectedTab === tab) return;
    this.selectedTab = tab;
    this.loadCurrentLicenses(this.currentOffsets[tab]);
  }

  loadCurrentLicenses(offset: number = 0): void {
    this.loading = true;
    this.error = '';
    this.currentOffsets[this.selectedTab] = offset;
    console.log('loadCurrentLicenses - loading tab:', this.selectedTab);

    let loader$: Observable<Page<License>>;
    switch (this.selectedTab) {
      case 'approved':
        loader$ = this.licenseService.getApprovedLicensesPage(offset, this.pageSize, this.searchTerm);
        break;
      case 'rejected':
        loader$ = this.licenseService.getRejectedLicensesPage(offset, this.pageSize, this.searchTerm);
        break;
      default:
        loader$ = this.licenseService.getPendingLicensesPage(offset, this.pageSize, this.searchTerm);
    }

    loader$.subscribe({
      next: (page: Page<License>) => {
        if (this.selectedTab === 'approved') {
          this.approvedLicenses = page;
        } else if (this.selectedTab === 'rejected') {
          this.rejectedLicenses = page;
        } else {
          this.pendingLicenses = page;
        }
        this.loading = false;
      },
      error: err => {
        this.error = err?.error?.message || `Failed to load ${this.selectedTab} licenses`;
        this.loading = false;
      }
    });
  }

  private matchesSearch(lic: License): boolean {
    if (!this.searchTerm) {
      return true;
    }
    return lic.username.toLowerCase().includes(this.searchTerm);
  }

  getCurrentList(): License[] {
    let page: Page<License>;
    switch (this.selectedTab) {
      case 'approved':
        page = this.approvedLicenses;
        break;
      case 'rejected':
        page = this.rejectedLicenses;
        break;
      default:
        page = this.pendingLicenses;
    }
    return page.content;
  }

  onApprove(license: License): void {
    if (!license.id) {
      this.error = 'Invalid license ID';
      return;
    }

    this.licenseService.updateStatus(license.id, 'APPROVED').subscribe({
      next: () => {
        this.notification.success('License approved successfully.');
        this.loadCurrentLicenses(); // refresh current page to reflect change
      },
      error: (err) => {
        console.error('Approving license failed', err);
        this.notification.error('Failed to approve license. Please try again.');
      }
    });
  }

  onReject(license: License): void {
    if (!license.id) {
      this.error = 'Invalid license ID';
      return;
    }

    this.licenseService.updateStatus(license.id, 'REJECTED').subscribe({
      next: () => {
        this.notification.success('License rejected successfully.');
        this.loadCurrentLicenses(); // refresh current page to reflect change
      },
      error: (err) => {
        console.error('Rejecting license failed', err);
        this.notification.error('Failed to reject license. Please try again.');
      }
    });
  }

  openFile(license: License): void {
    this.licenseService.downloadLicenseFile(license.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: (error) => console.error('Error downloading file', error)
    });
  }

  onPageChange(event: { first: number; rows: number }): void {
    const offset = event.first;
    const limit = event.rows;
    this.pageSize = limit;

    switch (this.selectedTab) {
      case 'approved':
        this.currentOffsets.approved = offset;
        this.licenseService.getApprovedLicensesPage(offset, limit, this.searchTerm).subscribe({
          next: page => this.approvedLicenses = page,
          error: err => this.handleError(err)
        });
        break;
      case 'rejected':
        this.currentOffsets.rejected = offset;
        this.licenseService.getRejectedLicensesPage(offset, limit, this.searchTerm).subscribe({
          next: page => this.rejectedLicenses = page,
          error: err => this.handleError(err)
        });
        break;
      default:
        this.currentOffsets.pending = offset;
        this.licenseService.getPendingLicensesPage(offset, limit, this.searchTerm).subscribe({
          next: page => this.pendingLicenses = page,
          error: err => this.handleError(err)
        });
    }
  }


  private handleError(err: any): void {
    this.loading = false;
    this.error = err?.error?.message || 'Failed to load licenses';
    console.error(err);
  }

}
