import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';
import { Observable, map, of } from 'rxjs';
import { JobOfferService } from '../services/job-offer.service';
import { ToastrService } from 'ngx-toastr';

@Injectable({
  providedIn: 'root'
})
export class OfferEditGuard  {

  constructor(
    private jobOfferService: JobOfferService,
    private router: Router,
    private notification: ToastrService
  ) { }

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean | UrlTree> {
    const offerId = +route.params['offerId'];
    if (isNaN(offerId)) {
      return of(this.router.createUrlTree(['/worker/offers']));
    }

    return this.jobOfferService.getOffers().pipe(
      map(offers => {
        const targetOffer = offers.find(offer => offer.id === offerId);

        // allow editing only if the offer exists and its status is PENDING
        if (targetOffer && targetOffer.status === 'PENDING') {
          return true;
        }

        // if offer is not editable, show a message and redirect
        this.notification.error('This offer cannot be edited because it is no longer pending.', 'Action Not Allowed');
        return this.router.createUrlTree(['/worker/offers']);
      })
    );
  }
}
