import {Component, OnInit} from '@angular/core';
import {UntypedFormBuilder, UntypedFormGroup, Validators} from '@angular/forms';
import {Router} from '@angular/router';
import {AuthService} from '../../services/auth.service';
import {AuthRequest} from '../../dtos/auth-request';
import {PushNotificationService} from "../../services/push-notification.service";


@Component({
    selector: 'app-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.scss'],
    standalone: false
})
export class LoginComponent implements OnInit {

  loginForm: UntypedFormGroup;
  // after first submission attempt, form validation will start
  submitted = false;
  error = false;
  errorMessage = '';
  errorTitle = 'Authentication problems!';

  constructor(private formBuilder: UntypedFormBuilder, private authService: AuthService, private router: Router, private pushService: PushNotificationService) {
    this.loginForm = this.formBuilder.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }

  /**
   * Form validation will start after the method is called, additionally an AuthRequest will be sent
   */
  loginUser() {
    this.submitted = true;
    if (this.loginForm.valid) {
      const authRequest: AuthRequest = new AuthRequest(this.loginForm.controls.username.value, this.loginForm.controls.password.value);
      this.authenticateUser(authRequest);
    } else {
      console.log('Invalid input');
    }
  }

  /**
   * Send authentication data to the authService. If the authentication was successfully, the user will be forwarded to the message page
   *
   * @param authRequest authentication data from the user login form
   */
  authenticateUser(authRequest: AuthRequest) {
    console.log('Try to authenticate user: ' + authRequest.username);
    this.authService.loginUser(authRequest).subscribe({
      next: () => {
        const username = this.authService.getUsername();
        const role = this.authService.getUserRole();
        console.log(`Successfully logged in user: ${username} with role: ${role}`);

        // push notifications
        this.pushService.subscribeToNotifications();
        this.pushService.listenToNotifications();

        if (role === 'WORKER') {
          this.router.navigate(['/worker']);
        } else if (role === 'CUSTOMER') {
          this.router.navigate(['/customer']);
        } else if (role === 'ADMIN') {
          this.router.navigate(['/admin/reports']);
        } else {
          this.router.navigate(['/message']);
        }
      },
      error: error => {
        console.log('Could not log in due to:', error);
        this.error = true;

        if (error.status === 401) {
          this.errorTitle = 'Authentication Failed';
          this.errorMessage = 'Invalid username or password.';
        } else if (error.status === 403) {
          this.errorTitle = 'Account Banned';
          this.errorMessage = 'Your account has been suspended. Please contact an administrator.';
        } else if (error.status === 0) {
          this.errorTitle = 'Connection Error';
          this.errorMessage = 'Could not connect to the server. It may be temporarily unavailable. Please try again later.';
        } else {
          // for any other server errors (f.e., 500 Internal Server Error)
          this.errorTitle = 'Login Error';
          this.errorMessage = `An unexpected error occurred. Please try again later. (Status: ${error.status})`;
        }
      }
    });
  }

  /**
   * Error flag will be deactivated, which clears the error message
   */
  vanishError() {
    this.error = false;
  }

  ngOnInit() {
  }

}
