import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private authService: AuthService,
              private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return false;
    }

    const requiredRole = route.data['requiredRole'];
    const userRole = this.authService.getUserRole();

    if (requiredRole && userRole !== requiredRole) {
      if (userRole == "CUSTOMER"){
        this.router.navigate(['/customer']);
      } else if (userRole == "WORKER") {
        this.router.navigate(['/worker']);
      }
      return false;
    }

    return true;
  }
}
