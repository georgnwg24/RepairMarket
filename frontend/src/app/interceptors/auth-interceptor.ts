import {Injectable} from '@angular/core';
import {HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse} from '@angular/common/http';
import {AuthService} from '../services/auth.service';
import {catchError, Observable, throwError} from 'rxjs';
import {Globals} from '../global/globals';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService, private globals: Globals) {
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

    // Do not intercept authentication requests
    if (req.url.endsWith('/authentication') || req.url.endsWith('/authentication/register')) {
      return next.handle(req);
    }

    const token = this.authService.getToken();
    // if token, add header
    const authReq = token
      ? req.clone({headers: req.headers.set('Authorization', `Bearer ${token}`)})
      : req;

    return next.handle(authReq).pipe(
      catchError((err: HttpErrorResponse)=> {
        if (err.status === 423) { // locked from jwtfilter
          this.authService.logoutUser();
          // this.router
        }
        return throwError(() => err);
      }),
    );
  }
}
