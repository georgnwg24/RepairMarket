import {Component, OnInit} from "@angular/core";
import {
  FormsModule, ReactiveFormsModule,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from "@angular/forms";
import { ViewChild } from '@angular/core';
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {ToastrService} from 'ngx-toastr';
import {PropertyService} from "../../services/property.service";
import {convertToPropertyCreateEdit, Property} from "../../dtos/property";
import {Observable} from "rxjs";
import {CommonModule} from "@angular/common";
import { AreaFinderComponent } from "../area-finder/area-finder.component";

export enum PropertyCreateEditMode {
  create,
  edit
}
@Component({
  selector: 'app-properties-create-edit',
  templateUrl: './properties-create-edit.component.html',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    AreaFinderComponent
  ],
  styleUrls: ['./properties-create-edit.component.scss']
})

export class PropertiesCreateEditComponent implements OnInit {
  propertyId: number | undefined;
  propertyForm: UntypedFormGroup;
  submitted = false;
  error = false;
  errorMessage = '';

  property: Property = {
    countryCode: null, postalCode: "", area: ""
  };

  mode = PropertyCreateEditMode.create
  @ViewChild(AreaFinderComponent) areaFinder!: AreaFinderComponent;

  constructor(
    private formBuilder: UntypedFormBuilder,
    private notification: ToastrService,
    private router: Router,
    private service: PropertyService,
    private route: ActivatedRoute,
  ) {
    this.propertyForm = this.formBuilder.group({
      countryCode: [null, Validators.required],
      address: ['', Validators.required],
      postalCode: [{value: '', disabled: true},  Validators.required],
      area: [{value: '', disabled: true}],
    });
  }

  get modeIsCreate(): boolean {
    return this.mode === PropertyCreateEditMode.create
  }

  ngOnInit(): void {
    this.route.data.subscribe(data => {
      this.mode = data.mode;
    });

    if (!this.modeIsCreate) {
      this.propertyId = Number(this.route.snapshot.paramMap.get('id'));
      this.service.getProperty(this.propertyId).subscribe({
        next: data => {
          this.property = data;
          this.propertyForm.patchValue(data);
          this.propertyForm.get('postalCode').enable();
          this.propertyForm.get('area').enable();

          // A small timeout ensures the view has updated before the check is triggered.
          setTimeout(() => {
            this.areaFinder.triggerInitialStateCheck();
          }, 0);
        },
        error: err => {
          this.error = true;
          this.errorMessage = "Could not load property details. Please try again later.";
          this.propertyForm.disable();
        }
      });
    }
  }

  get form() {
    return this.propertyForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.vanishError();

    if (this.propertyForm.invalid) {
      return;
    }

    Object.assign(this.property, this.propertyForm.getRawValue());

    let observable: Observable<Property>;
    if(this.modeIsCreate){
      observable = this.service.createProperty(convertToPropertyCreateEdit(this.property))
    } else {
      observable = this.service.updateProperty(convertToPropertyCreateEdit(this.property), this.propertyId)
    }

    observable.subscribe({
      next: () => {
        const message = this.modeIsCreate ? 'successfully added Property!' : 'successfully saved changes!';
        this.notification.success(message, '', {
          timeOut: 3000,
          closeButton: true,
        });
        this.router.navigate([`customer/edit`]);
      },
      error: err => {
        this.error = true;
        if (err.status === 400 && Array.isArray(err.error)) {
          let nonFieldErrors: string[] = [];
          err.error.forEach((validationError: any) => {
            const control = this.propertyForm.get(validationError.field);
            if (control) {
              control.setErrors({ serverError: validationError.defaultMessage });
            } else {
              nonFieldErrors.push(validationError.defaultMessage);
            }
          });
          this.errorMessage = nonFieldErrors.join('<br>');
        } else {
          // handles other types of errors (network, 500, etc.)
          this.errorMessage = err.error?.message || 'An unexpected error occurred. Please try again.';
        }
      }
    });
  }


  public get submitButtonText(): string {
    switch (this.mode) {
      case PropertyCreateEditMode.create:
        return 'Add Address';
      case PropertyCreateEditMode.edit:
        return 'Save Changes';
      default:
        return '?';
    }
  }

  vanishError(): void {
    this.error = false;
  }
}
