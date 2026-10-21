import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { ReportDetail } from 'src/app/dtos/report';
import { ReportService } from 'src/app/services/report.service';
import { DatePipe, NgIf, TitleCasePipe} from "@angular/common";
import {environment} from "../../../environments/environment";
import {PdfViewerModule} from "ng2-pdf-viewer";
import {ToastrService} from "ngx-toastr";

@Component({
  selector: 'app-report-detail',
  templateUrl: './report-detail.component.html',
  imports: [
    DatePipe,
    TitleCasePipe,
    NgIf,
    PdfViewerModule,
    RouterLink
  ],
  styleUrls: ['./report-detail.component.scss']
})
export class ReportDetailComponent implements OnInit {
  report: ReportDetail | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private reportService: ReportService,
    private notification: ToastrService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe((params: ParamMap) => {
      const idParam = params.get('reportId') || params.get('id');
      const reportId = idParam ? +idParam : NaN;
      if (isNaN(reportId)) {
        this.onBack();
      } else {
        this.loadReport(reportId);
      }
    });
  }

  private loadReport(reportId: number): void {
    this.reportService.getReportDetail(reportId).subscribe({
      next: dto => this.report = dto,
      error: err => {
        console.error('Failed to load report', err);
      }
    });
  }

  onBack(): void {
    this.router.navigate(['/admin/reports']);
  }

  closeReport(): void {
    if (!this.report.isOpen) {
      this.notification.warning('Report is already closed.');
      return;
    }
    this.reportService.closeReport(this.report.id).subscribe({
      next: () => {
        this.notification.success('Report closed successfully.');
        this.report.isOpen = false;
      },
      error: (err: any) => {
        console.error('Closing report failed', err);
        this.notification.error('Failed to close report. Please try again.');
      }
    });
  }

  openReport(): void {
    if (this.report.isOpen) {
      this.notification.warning('Report is already opened.');
      return;
    }
    this.reportService.openReport(this.report.id).subscribe({
      next: () => {
        this.notification.success('Report opened successfully.');
        this.report.isOpen = true;
      },
      error: (err: any) => {
        console.error('Opening report failed', err);
        this.notification.error('Failed to open report. Please try again.');
      }
    });
  }

  formatDisplayString(input: string): string {
    if (!input) {
      return '';
    }
    return input.charAt(0).toUpperCase() + input.slice(1).toLowerCase();
  }

  protected readonly environment = environment;
}
