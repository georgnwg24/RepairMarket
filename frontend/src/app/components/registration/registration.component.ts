import {Component, OnInit} from "@angular/core";
import {AbstractControl, ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, ValidatorFn, Validators} from "@angular/forms";
import {AuthService} from "../../services/auth.service";
import {Router, RouterLink} from "@angular/router";
import {UserRegistrationDto} from "../../dtos/UserRegistrationDto";
import {ToastrService} from 'ngx-toastr';
import { CommonModule } from "@angular/common";

import { AreaFinderComponent } from '../area-finder/area-finder.component';

@Component({
  selector: 'app-register',
  templateUrl: './registration.component.html',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    AreaFinderComponent
  ],
  styleUrls: ['./registration.component.scss']
})
export class RegistrationComponent implements OnInit {
  registrationForm: UntypedFormGroup;
  submitted = false;
  error = false;
  errorMessage = '';

  isWorker = false;

  constructor(
    private formBuilder: UntypedFormBuilder,
    private notification: ToastrService,
    private authService: AuthService,
    private router: Router,
  ) {
    const mustMatch: ValidatorFn = (c: AbstractControl) => {
      const pw = c.get('password');
      const cpw = c.get('confirmPassword');
      return pw && cpw && pw.value === cpw.value ? null : { mustMatch: true };
    };

    this.registrationForm = this.formBuilder.group({
      username: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required],
      role: ['', Validators.required],
      firstName: [''],
      lastName: [''],
      countryCode: [{value: null, disabled: true}],
      postalCode: [{value: '', disabled: true}],
      area: [{value: '', disabled: true}],
    }, { validators: mustMatch });
  }

  ngOnInit(): void {
    this.registrationForm.get('role')?.valueChanges.subscribe(role => {
      this.isWorker = (role === 'WORKER');
      const addressControls = ['countryCode', 'postalCode', 'area'];

      if (this.isWorker) {
        addressControls.forEach(controlName => {
          this.registrationForm.get(controlName)?.setValidators(Validators.required);
        });
        this.registrationForm.get('countryCode')?.enable();
      } else {
        addressControls.forEach(controlName => {
          this.registrationForm.get(controlName)?.clearValidators();
          this.registrationForm.get(controlName)?.disable();
          this.registrationForm.get(controlName)?.reset({ value: null, disabled: true });
        });
      }
      this.registrationForm.updateValueAndValidity();
    });
  }

  get form() {
    return this.registrationForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = false;
    if (this.registrationForm.invalid) {
      return;
    }

    const formValue = this.registrationForm.getRawValue();
    const dto: UserRegistrationDto = {
      username: formValue.username,
      email:    formValue.email,
      password: formValue.password,
      confirmPassword: formValue.confirmPassword,
      role:     formValue.role,
      firstName: formValue.firstName,
      lastName:  formValue.lastName,
      countryCode: this.isWorker ? formValue.countryCode : undefined,
      postalCode:  this.isWorker ? formValue.postalCode : undefined,
      area:        this.isWorker ? formValue.area : undefined,
    };

    this.authService.registerUser(dto).subscribe({
      next: () => {
        this.notification.success(`${dto.username} successfully registered!`, '', {
          timeOut: 3000,
          closeButton: true,
        });
        this.router.navigate(['/login']);
      },
      error: err => {
        // checks for backend validation errors (HTTP 400)
        if (err.status === 400 && Array.isArray(err.error)) {
          err.error.forEach((validationError: any) => {
            // 'field' is present for field-specific errors
            if (validationError.field) {
              const formControl = this.registrationForm.get(validationError.field);
              if (formControl) {
                // attaches the server message to the form control
                formControl.setErrors({ serverError: validationError.defaultMessage });
              }
            } else {
              // handles class-level errors (e.g., from @AssertTrue)
              this.error = true;
              this.errorMessage = validationError.defaultMessage;
            }
          });
        } else if (err.status === 409) {
          // handles User/Email exists errors from GlobalExceptionHandler (HTTP 409)
          this.error = true;
          this.errorMessage = err.error;
        } else {
          this.error = true;
          this.errorMessage = err.error?.message || 'An unexpected error occurred.';
        }
      }
    });
  }

  vanishError(): void {
    this.error = false;
  }
}
