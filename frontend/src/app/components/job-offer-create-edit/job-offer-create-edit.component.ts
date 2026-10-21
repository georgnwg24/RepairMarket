import { Component, OnInit } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { JobOfferService } from '../../services/job-offer.service';
import { JobOfferCreate } from '../../dtos/jobOffer';
import { CommonModule, Location } from '@angular/common';

@Component({
  selector: 'app-job-offer-create-edit',
  templateUrl: './job-offer-create-edit.component.html',
  styleUrls: ['./job-offer-create-edit.component.scss'],
  standalone: true,
  imports: [
    FormsModule,
    CommonModule
  ]
})
export class JobOfferCreateEditComponent implements OnInit {
  jobOffer: JobOfferCreate = { price: null!, comment: '' };
  loading = false;
  error = '';
  requestId!: number;
  submitButtonText = 'Create Offer';
  formTitle = 'Create Offer';
  offerId?: number;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private jobOfferService: JobOfferService,
    private location: Location
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['requestId']) {
        this.requestId = +params['requestId'];
      }
      if (params['offerId']) {
        this.offerId = +params['offerId'];
        this.submitButtonText = 'Save Changes';
        this.formTitle = 'Edit Offer';
        this.jobOfferService.getSingleOffer(this.offerId).subscribe({
          next: (data) => this.jobOffer = data,
          error: (err) => this.error = err
        });
      }
    });
  }

  onSubmit(form: NgForm): void {
    if (!form.valid) {
      Object.values(form.controls).forEach(control => {
        control.markAsTouched();
      });
      return;
    }

    this.loading = true;
    this.error = ''; // Clear previous errors on a new submission

    const handleSuccess = () => {
      const destination = this.offerId ? ['/worker', 'offers'] : ['/worker', 'requests'];
      this.router.navigate(destination);
    };

    const handleError = (err: any) => {
      // check if the error is a 400 Bad Request with a list of validation errors
      if (err.status === 400 && Array.isArray(err.error)) {
        // map the error messages to a single string with line breaks
        this.error = err.error
          .map((e: any) => e.defaultMessage || 'A validation error occurred.')
          .join('<br>');
      } else {
        // fallback for other errors (network, server issues, etc.)
        this.error = err.error?.message || err.error || err.message || 'An unexpected error occurred.';
      }
      this.loading = false;
    };

    if (this.offerId) {
      this.jobOfferService.updateOffer(this.offerId, this.jobOffer).subscribe({
        next: handleSuccess,
        error: handleError
      });
    } else {
      this.jobOfferService.create(this.requestId, this.jobOffer).subscribe({
        next: handleSuccess,
        error: handleError
      });
    }
  }

  goBack(): void {
    this.location.back();
  }
}
