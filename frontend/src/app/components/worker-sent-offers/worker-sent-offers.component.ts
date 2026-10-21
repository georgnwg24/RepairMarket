import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Router, ActivatedRoute } from '@angular/router';
import {CommonModule} from "@angular/common";
import {JobOfferSummary} from "../../dtos/jobOffer";
import {JobOfferService} from "../../services/job-offer.service";
import {JobOfferStatus} from "../../dtos/JobOfferStatus";
import {ConfirmDeleteDialogComponent} from "../confirmation-dialogues/confirm-delete-dialog/confirm-delete-dialog.component";
import {ConfirmWithdrawDialogComponent} from "../confirmation-dialogues/confirm-withdraw-dialog/confirm-withdraw-dialog.component";
import {ChatService} from "../../services/chat.service";
import {Page} from "../../dtos/page";
import {JobRequestPrice} from "../../dtos/jobRequest";
import {Paginator, PaginatorState} from "primeng/paginator";

@Component({
  selector: 'app-worker-sent-offers',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    CommonModule,
    ConfirmDeleteDialogComponent,
    ConfirmWithdrawDialogComponent,
    Paginator
  ],
  templateUrl: './worker-sent-offers.component.html',
  styleUrl: './worker-sent-offers.component.scss'
})
export class WorkerSentOffersComponent implements OnInit {
  pageSize = 10;
  jobOfferPage: Page<JobOfferSummary> = {
    content: [],
    totalElements: 0,
    pageSize: 0,
    offset: 0
  };
  loading = false;
  error = '';
  offerForDeletion: JobOfferSummary | undefined;
  offerForWithdrawal: JobOfferSummary | undefined;

  constructor(
    private route: ActivatedRoute,
    private service: JobOfferService,
    private chatService: ChatService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.jobOfferPage.offset = +params['offset'] || 0;
      this.jobOfferPage.pageSize = +params['limit'] || this.pageSize;

      this.loadOffers();
    });
  }

  loadOffers(): void {
    this.loading = true;
    this.error = null;

    let offset = this.jobOfferPage.offset;
    let limit = this.jobOfferPage.pageSize;

    this.service.getOffersFromWorker(offset, limit).subscribe({
      next: (data) => {
        console.log(data);
        this.jobOfferPage = data
        this.loading = false;
      },
      error: (err)   => {
        this.error = err?.message || 'Failed to load offers.';
        this.loading = false;
      }
    });
  }
  withdrawOffer(offerId: number): void {
    this.service.withdrawOffer(offerId).subscribe(() => this.loadOffers());
  }

  deleteOffer(offerId: number): void {
    this.service.deleteOffer(offerId).subscribe(() => this.loadOffers());
  }

  isWithdrawable(status: JobOfferStatus): boolean {
    return status === 'PENDING';
  }

  isDeletable(status: JobOfferStatus): boolean {
    return status === 'REJECTED' || status === 'WITHDRAWN' || status === 'DONE';
  }

  getRowClass(status: string): string {
    switch (status) {
      case 'ACCEPTED':
      case 'DONE':
        return 'table-row-success';
      case 'REJECTED':
        return 'table-row-danger';
      case 'WITHDRAWN':
        return 'table-row-warning';
      case 'PENDING':
      default:
        return '';
    }
  }

  openChat(jobRequestId: number) {
    this.chatService.engageConversation(jobRequestId).subscribe({
      next: value => this.router.navigate(["/chats", value.chatId]),
      error: err => console.log(err),
    });
  }

  onPageChange(event: PaginatorState): void {
    const newOffset = event.first ?? 0;
    const newLimit = event.rows ?? this.jobOfferPage.pageSize;
    this.jobOfferPage.offset = newOffset;
    this.jobOfferPage.pageSize = newLimit;

    this.loadOffers();
  }

}
