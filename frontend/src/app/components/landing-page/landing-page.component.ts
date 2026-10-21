import { Component, OnInit } from '@angular/core';
import {ReactiveFormsModule} from "@angular/forms";
import { ButtonModule } from 'primeng/button';
import {Router, RouterLink} from "@angular/router";
import { AuthService } from '../../services/auth.service';

@Component({
  templateUrl: './landing-page.component.html',
  styleUrls: ['./landing-page.component.scss'],
  standalone: true,
  imports: [
    ButtonModule,
    ReactiveFormsModule,
    RouterLink
  ]
})
export class LandingPageComponent implements OnInit {


  ngOnInit(): void {
    // Check if the user is logged in
    if (this.authService.isLoggedIn()) {
      const userRole = this.authService.getUserRole();
      let redirectPath = '';

      // Determine the correct redirect path based on the user's role
      switch (userRole) {
        case 'CUSTOMER':
          redirectPath = '/customer/requests'; //
          break;
        case 'WORKER':
          redirectPath = '/worker/requests'; //
          break;
        case 'ADMIN':
          redirectPath = '/admin/reports'; //
          break;
        default:
          return;
      }

      this.router.navigate([redirectPath]);
    }
  }

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  goToRegister() {
    this.router.navigate(['/register']);
  }

}
