import {Component, OnInit} from '@angular/core';
import {PropertyCreateEditMode} from "../properties-create-edit/properties-create-edit.component";
import {FormsModule, ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, Validators} from "@angular/forms";
import {ToastrService} from "ngx-toastr";
import {ActivatedRoute, Router} from "@angular/router";
import {Observable} from "rxjs";
import {CommonModule, NgClass} from "@angular/common";
import {Role} from "../../dtos/UserRegistrationDto";
import {RatingService} from "../../services/rating.service";
import {Rating} from "../../dtos/rating";

@Component({
  selector: 'app-ratings-create-edit',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    NgClass,
    CommonModule,
  ],
  templateUrl: './ratings-create-edit.component.html',
  standalone: true,
  styleUrl: './ratings-create-edit.component.scss'
})
export class RatingsCreateEditComponent implements OnInit {
  mode = PropertyCreateEditMode.create;
  ratingForm: UntypedFormGroup;
  jobRequestId: number | undefined;
  hovered = 0;
  stars = Array(5).fill(0);
  role: Role;

  constructor(
    private formBuilder: UntypedFormBuilder,
    private notification: ToastrService,
    private router: Router,
    private service: RatingService,
    private route: ActivatedRoute,
  ) {

    this.ratingForm = this.formBuilder.group({
      stars: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
      comment: ['', [Validators.maxLength(1023)]]
    });
  }

  get modeIsCreate(): boolean {
    return this.mode === PropertyCreateEditMode.create
  }

  ngOnInit(): void {
    this.route.data.subscribe(data => {
      this.role = data.requiredRole;
    });
    this.jobRequestId = Number(this.route.snapshot.paramMap.get('requestId'))

    // check if create or edit mode
    this.service.getRating(this.jobRequestId).subscribe({
      next: (data) => {
        if (data) {
          this.mode = PropertyCreateEditMode.edit;
          this.ratingForm.patchValue({
            stars: data.stars,
            comment: data.comment
          });
        } else {
          this.mode = PropertyCreateEditMode.create;
        }
      },
      error: (err) => {
        console.error("Error loading rating:", err);
        this.mode = PropertyCreateEditMode.create;
      }
    });

  }

  onSubmit(): void {
    if (this.ratingForm.invalid) {
      return;
    }
    const rating: Rating = {
      stars: this.ratingForm.value.stars,
      comment: this.ratingForm.value.comment
    };

    let observable: Observable<any>;
    if(this.modeIsCreate){
      observable = this.service.createRating(rating, this.jobRequestId)
    } else {
      observable = this.service.updateRating(rating, this.jobRequestId)
    }
    observable.subscribe({
      next: () => {
        if (this.modeIsCreate){
          this.notification.success(`successfully submitted Rating!`, '', {
            timeOut: 3000,
            closeButton: true,
          });
        } else {
          this.notification.success(`successfully saved changes!`, '', {
            timeOut: 3000,
            closeButton: true,
          });
        }

        if (this.role === Role.CUSTOMER) {
          this.router.navigate(['/customer/requests']);
        } else if (this.role === Role.WORKER) {
          this.router.navigate(['/worker/offers']);
        }
      },
    });
  }

  public get submitButtonText(): string {
    switch (this.mode) {
      case PropertyCreateEditMode.create:
        return 'Submit Rating';
      case PropertyCreateEditMode.edit:
        return 'Save Changes';
      default:
        return '?';
    }
  }

  public cancel(): void {
    if (this.role === Role.CUSTOMER) {
      this.router.navigate(['/customer/requests']);
    } else if (this.role === Role.WORKER) {
      this.router.navigate(['/worker/offers']);
    }
  }
}
