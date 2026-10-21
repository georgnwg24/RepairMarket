import {Component, OnInit} from "@angular/core";
import {
  FormsModule, ReactiveFormsModule,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from "@angular/forms";
import {AuthService} from "../../services/auth.service";
import { ViewChild } from '@angular/core';
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {Role} from "../../dtos/UserRegistrationDto";
import {ToastrService} from 'ngx-toastr';
import {convertToUpdate, User,} from "../../dtos/user";
import {UserService} from "../../services/user.service";
import {PropertyService} from "../../services/property.service";
import {Property} from "../../dtos/property";
import {CommonModule} from "@angular/common";
import {ConfirmDeleteDialogComponent} from "../confirmation-dialogues/confirm-delete-dialog/confirm-delete-dialog.component";
import {License} from "../../dtos/license";
import {LicenseService} from "../../services/license.service";
import {saveAs} from "file-saver";
import { switchMap } from 'rxjs/operators';
import { AreaFinderComponent } from '../area-finder/area-finder.component';

@Component({
  selector: 'app-user-edit',
  templateUrl: './user-edit.component.html',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ConfirmDeleteDialogComponent,
    AreaFinderComponent,
    RouterLink
  ],
  styleUrls: ['./user-edit.component.scss']
})
export class UserEditComponent implements OnInit {
  updateUserForm: UntypedFormGroup;
  submitted = false;
  error = false;
  errorMessage = '';
  mode: Role = Role.CUSTOMER;
  user: User = {
    area: "", email: "", firstName: "", lastName: "", username: ""
  };
  username: string | undefined
  properties: Property[] = [];
  licenses: License[] = [];
  propertyForDeletion: Property | undefined;
  licenseForDeletion: License | undefined;
  @ViewChild(AreaFinderComponent) areaFinder!: AreaFinderComponent;

  constructor(
    private formBuilder: UntypedFormBuilder,
    private notification: ToastrService,
    protected authService: AuthService,
    private router: Router,
    private service: UserService,
    private propertyService: PropertyService,
    private licenseService: LicenseService,
    private route: ActivatedRoute,
  ) {
    this.updateUserForm = this.formBuilder.group({
      username: [{value: '', disabled: true}, [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      firstName: ['', Validators.max(255)],
      lastName: ['', Validators.max(255)],
      countryCode: [''],
      postalCode: [''],
      area: ['', Validators.max(255)],
    });
  }

  get modeIsCustomer(): boolean {
    return this.mode === Role.CUSTOMER;
  }

  ngOnInit(): void {
    this.route.data.pipe(
      switchMap(data => {
        this.mode = data.mode as Role;
        return this.service.getUserForUpdateByUsername();
      })
    ).subscribe(userData => {
      this.user = userData;
      this.username = userData.username;

      if (this.modeIsCustomer) {
        this.updateUserForm.get('countryCode')?.clearValidators();
        this.updateUserForm.get('postalCode')?.clearValidators();
        this.updateUserForm.get('area')?.setValidators([Validators.max(255)]);
      } else { // Worker
        this.updateUserForm.get('countryCode')?.setValidators(Validators.required);
        this.updateUserForm.get('postalCode')?.setValidators(Validators.required);
        this.updateUserForm.get('area')?.setValidators([Validators.required, Validators.max(255)]);
      }
      this.updateUserForm.updateValueAndValidity();

      this.updateUserForm.patchValue({
        username: userData.username,
        email: userData.email,
        firstName: userData.firstName,
        lastName: userData.lastName,
        countryCode: userData.countryCode,
        postalCode: userData.postalCode,
        area: userData.area
      });
      if (this.areaFinder) {
        this.areaFinder.triggerInitialStateCheck();
      }

      if (this.modeIsCustomer) {
        this.propertyService.listProperties().subscribe(data => this.properties = data);
      } else { // Worker
        this.licenseService.listCertificate(userData.username).subscribe(data => this.licenses = data);
      }
    });
  }

  downloadFile(license: License): void {
    this.licenseService.downloadLicenseFile(license.id).subscribe(
      (blob) => saveAs(blob, license.filename),
      (error) => console.error('Error downloading file', error)
    );
  }

  reloadAdress() {
    this.propertyService.listProperties().subscribe(data => this.properties = data);
  }

  reloadLicenses() {
    this.licenseService.listCertificate(this.username).subscribe(data => this.licenses = data);
  }

  isApproved(license: License): boolean {
    return license.status === 'APPROVED';
  }

  get form() {
    return this.updateUserForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.vanishError();

    if (this.updateUserForm.invalid) {
      Object.keys(this.updateUserForm.controls).forEach(key => {
        const controlErrors = this.updateUserForm.get(key)?.errors;
        if (controlErrors != null) console.error('Key: ' + key + ', Error: ', controlErrors);
      });
      return;
    }

    const formValue = this.updateUserForm.getRawValue();

    if (this.modeIsCustomer) {
      if (!formValue.postalCode) formValue.postalCode = 'N/A';
      if (!formValue.area) formValue.area = 'N/A';
    }

    Object.assign(this.user, formValue);

    this.service.updateUser(convertToUpdate(this.user)).subscribe({
      next: () => {
        this.notification.success(`Information for ${this.user.username} successfully changed!`, '', {
          timeOut: 3000,
          closeButton: true,
        });

        if (this.modeIsCustomer) {
          this.router.navigate([`customer/`]);
        } else {
          this.router.navigate([`worker/`]);
        }
      },
      error: err => {
        this.error = true;
        if (err.status === 400 && Array.isArray(err.error)) {
          // handles backend validation errors
          let nonFieldErrors: string[] = [];
          err.error.forEach((validationError: any) => {
            if (validationError.field) {
              const formControl = this.updateUserForm.get(validationError.field);
              if (formControl) {
                formControl.setErrors({ serverError: validationError.defaultMessage });
              }
            } else {
              nonFieldErrors.push(validationError.defaultMessage);
            }
          });
          this.errorMessage = nonFieldErrors.join('<br>');

        } else if (err.status === 409) {
          this.errorMessage = err.error?.message || err.error;
        } else {
          // handles other errors like network issues or 500s
          this.errorMessage = err.error?.message || 'An unexpected error occurred. Please try again.';
        }
      }
    });
  }

  deleteProperty(property: Property) {
    this.propertyService.deleteProperty(property.id).subscribe({
      next: () => {
        this.notification.success("Successfully deleted Property", 'Delete Successful');
        this.reloadAdress();
      }
    });
  }

  deleteLicense(license: License) {
    this.licenseService.deleteCertificate(license.id).subscribe({
      next: () => {
        this.notification.success("Successfully deleted License", 'Delete Successful');
        this.reloadLicenses();
      }
    });
  }

  onDeleteAccount(): void {
    this.authService.deleteUserByUsername().subscribe({
      next: () => {
        this.authService.logoutUser();
        this.router.navigate(['/login']);
      },
      error: (err) => {
        console.error('Delete failed:', err);
        alert('Could not delete account: ' + (err.error?.message || err.statusText));
      }
    });
  }

  vanishError(): void {
    this.error = false;
  }
}
