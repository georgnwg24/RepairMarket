import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JobRequestPrice } from '../../dtos/jobRequest';

export interface JobCardData extends JobRequestPrice {
  imageUrl?: string;
  categoryDisplayName?: string;
  deadlineLocale?: string;
}


@Component({
  selector: 'app-job-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './job-request-datacard.component.html',
  styleUrls: ['./job-request-datacard.component.scss'],

  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JobRequestDatacardComponent {
  @Input({ required: true }) job!: JobCardData;

  @Input() mode: 'worker' | 'customer' | 'admin' = 'worker';

  @Output() viewDetails = new EventEmitter<JobCardData>();
  @Output() createOffer = new EventEmitter<JobCardData>();
  @Output() report = new EventEmitter<JobCardData>();
  @Output() edit = new EventEmitter<JobCardData>();
  @Output() delete = new EventEmitter<JobCardData>();

  constructor() {}

  onViewDetailsClick(): void {
    this.viewDetails.emit(this.job);
  }

  onCreateOfferClick(): void {
    this.createOffer.emit(this.job);
  }

  onReportClick(): void {
    this.report.emit(this.job);
  }

  onEditClick(): void {
    this.edit.emit(this.job);
  }

  onDeleteClick(): void {
    this.delete.emit(this.job);
  }
}
