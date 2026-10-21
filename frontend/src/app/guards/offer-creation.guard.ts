import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, switchMap, catchError } from 'rxjs/operators';
import { JobOfferService } from '../services/job-offer.service';
import { ToastrService } from 'ngx-toastr';
import { JobRequestService } from '../services/job-request.service';

@Injectable({
  providedIn: 'root'
})
export class OfferCreationGuard  {

  constructor(
    private jobOfferService: JobOfferService,
    private jobRequestService: JobRequestService,
    private router: Router,
    private notification: ToastrService
  ) {}

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean | UrlTree> {
    const requestId = +route.params['requestId'];
    if (isNaN(requestId)) {
      return of(this.router.createUrlTree(['/worker/requests']));
    }

    // Step 1: check if the worker already has an offer for this request
    return this.jobOfferService.getOffers().pipe(
      switchMap(offers => {
        const hasExistingOffer = offers.some(offer => offer.jobRequestId === requestId);
        if (hasExistingOffer) {
          this.notification.warning('You have already made an offer for this job request.', 'Offer Exists');
          // If an offer already exists, stop here and redirect
          return of(this.router.createUrlTree(['/worker/requests']));
        }

        // Step 2: if no offer exists, proceed to check the job request's status
        return this.jobRequestService.getById(requestId).pipe(
          map(jobRequest => {
            if (jobRequest.status === 'PENDING') {
              // if the status is PENDING, allow navigation
              return true;
            } else {
              // if status is not PENDING, block navigation
              this.notification.error('Offers can only be made for job requests that are pending.', 'Action Not Allowed');
              return this.router.createUrlTree(['/worker/requests']);
            }
          })
        );
      }),
      catchError(() => {
        // this will catch errors from either getOffersFromWorker() or getById()
        this.notification.error('Could not verify job request details.', 'Error');
        return of(this.router.createUrlTree(['/worker/requests']));
      })
    );
  }
}
