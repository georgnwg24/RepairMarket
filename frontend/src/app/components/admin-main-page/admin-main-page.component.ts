import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ReportService} from 'src/app/services/report.service';
import {ReportListDto} from 'src/app/dtos/report';
import {ToastrService} from "ngx-toastr";
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {PaginatorModule, PaginatorState} from 'primeng/paginator';
import {Page} from "../../dtos/page";
import {ChatWindowComponent} from "../chat/chat-window/chat-window.component";
import {ReactiveFormsModule, FormControl} from '@angular/forms';
import {debounceTime, distinctUntilChanged} from "rxjs/operators";
import {forkJoin} from "rxjs";

@Component({
  selector: 'admin-main-page',
  templateUrl: './admin-main-page.component.html',
  standalone: true,
  styleUrls: ['./admin-main-page.component.scss'],
  imports: [CommonModule, RouterLink, PaginatorModule, ReactiveFormsModule]
})
export class AdminMainPageComponent implements OnInit {
  openReportsPage: Page<ReportListDto> = {
    content: [],
    totalElements: 0,
    pageSize: 0,
    offset: 0
  };
  closedReportsPage: Page<ReportListDto> = {
    content: [],
    totalElements: 0,
    pageSize: 0,
    offset: 0
  };
  // reports: ReportListDto[] = [];
  pageSize = 10;
  loading = false;
  error = '';
  selectedTab: 'open' | 'closed' = 'open';
  searchControl = new FormControl('');
  private searchTerm = '';

  constructor(
    private reportService: ReportService,
    private notification: ToastrService,
    private route: ActivatedRoute,
    private router: Router
  ) {
  }

  ngOnInit(): void {

    this.route.queryParams.subscribe(params => {
      this.selectedTab = params['tab'] === 'closed' ? 'closed' : 'open';

      this.openReportsPage.offset = +params['openOffset'] || 0;
      this.openReportsPage.pageSize = +params['openLimit'] || this.pageSize;

      this.closedReportsPage.offset = +params['closedOffset'] || 0;
      this.closedReportsPage.pageSize = +params['closedLimit'] || this.pageSize;

      this.reloadReports();
    });

    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(val => {
        this.searchTerm = (val || '').trim().toLowerCase();

        if (this.selectedTab === 'open') {
          this.openReportsPage.offset = 0;
        } else {
          this.closedReportsPage.offset = 0;
        }

        this.reloadReports();
      });
  }


  reloadReports(): void {
    this.loading = true;
    this.error = '';

    const isOpen = this.selectedTab === 'open';
    const report$ = this.reportService.getPageOfReports(
      isOpen ? this.openReportsPage.offset : this.closedReportsPage.offset,
      isOpen ? this.openReportsPage.pageSize : this.closedReportsPage.pageSize,
      isOpen,
      this.searchTerm
    );

    report$.subscribe({
      next: (page) => {
        if (isOpen) {
          this.openReportsPage = page;
        } else {
          this.closedReportsPage = page;
        }
        this.loading = false;
      },
      error: (err) => {
        this.error = err;
        this.loading = false;
      }
    });
  }


  onCloseReport(report: ReportListDto): void {
    if (!report.isOpen) {
      this.notification.warning('Report is already closed.');
      return;
    }
    this.reportService.closeReport(report.id).subscribe({
      next: () => {
        this.notification.success('Report closed successfully.');
        this.reloadReports();
      },
      error: (err: any) => {
        console.error('Closing report failed', err);
        this.notification.error('Failed to close report. Please try again.');
      }
    });
  }

  onOpenReport(report: ReportListDto): void {
    if (report.isOpen) {
      this.notification.warning('Report is already opened.');
      return;
    }
    this.reportService.openReport(report.id).subscribe({
      next: () => {
        this.notification.success('Report opened successfully.');
        this.reloadReports();
      },
      error: (err: any) => {
        console.error('Opening report failed', err);
        this.notification.error('Failed to open report. Please try again.');
      }
    });
  }

  reportedAtAsLocaleString(report: ReportListDto): string {
    if (report.reportedAt) {
      return new Date(report.reportedAt).toLocaleString();
    }
    return '';
  }

  onPageChange(event: PaginatorState): void {
    const updatedParams: any = {
      tab: this.selectedTab
    };

    if (this.selectedTab === 'open') {
      this.openReportsPage.offset = event.first ?? 0;
      this.openReportsPage.pageSize = event.rows ?? this.pageSize;
      updatedParams.openOffset = this.openReportsPage.offset;
      updatedParams.openLimit = this.openReportsPage.pageSize;
      updatedParams.closedOffset = this.closedReportsPage.offset;
      updatedParams.closedLimit = this.closedReportsPage.pageSize;
    } else {
      this.closedReportsPage.offset = event.first ?? 0;
      this.closedReportsPage.pageSize = event.rows ?? this.pageSize;
      updatedParams.closedOffset = this.closedReportsPage.offset;
      updatedParams.closedLimit = this.closedReportsPage.pageSize;
      updatedParams.openOffset = this.openReportsPage.offset;
      updatedParams.openLimit = this.openReportsPage.pageSize;
    }
    this.reloadReports();
  }
}
