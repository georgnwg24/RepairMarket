import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { JobRequestService } from '../services/job-request.service';
import { ToastrService } from 'ngx-toastr';

@Injectable({
  providedIn: 'root'
})
export class RequestEditGuard {

  constructor(
    private jobRequestService: JobRequestService,
    private router: Router,
    private notification: ToastrService
  ) {}

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean | UrlTree> {
    const requestId = +route.params['id'];
    if (isNaN(requestId)) {
      // if the ID is invalid, redirect immediately
      return of(this.router.createUrlTree(['/customer/requests']));
    }

    // fetch the specific job request from the backend
    return this.jobRequestService.getById(requestId).pipe(
      map(jobRequest => {
        // allow editing only if the job request status is PENDING
        if (jobRequest.status === 'PENDING') {
          return true;
        } else {
          // if the status is not PENDING, show an error and redirect
          this.notification.error('This job request cannot be edited because it is no longer pending.', 'Action Not Allowed');
          return this.router.createUrlTree(['/customer/requests']);
        }
      }),
      catchError(() => {
        // if the job request is not found or another error occurs, redirect
        this.notification.error('Could not find the specified job request.', 'Error');
        return of(this.router.createUrlTree(['/customer/requests']));
      })
    );
  }
}
