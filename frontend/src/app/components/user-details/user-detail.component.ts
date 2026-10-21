import {Component, OnInit} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";
import {Role} from "../../dtos/UserRegistrationDto";
import {User,} from "../../dtos/user";
import {UserService} from "../../services/user.service";
import {NgClass, NgForOf, NgIf} from "@angular/common";
import {LicenseService} from "../../services/license.service";
import {License} from "../../dtos/license";
import {saveAs} from 'file-saver'
import {FormsModule} from "@angular/forms";
import {Rating, RatingStats} from "../../dtos/rating";
import {RatingService} from "../../services/rating.service";

@Component({
  selector: 'app-user-detail',
  templateUrl: './user-detail.component.html',
  standalone: true,
  imports: [
    NgForOf,
    NgIf,
    FormsModule,
    NgClass
  ],
  styleUrls: ['./user-detail.component.scss']
})

export class UserDetailComponent implements OnInit {
  userId: number | undefined
  username: string | undefined
  error = false;
  errorMessage = '';
  mode: Role = Role.CUSTOMER;
  user: User = {
    area: "", firstName: "", lastName: "", username: ""
  };
  licenses: License[] = [];
  ratings: Rating[] = [];
  ratingStats: RatingStats | undefined;

  constructor(
    private router: Router,
    private service: UserService,
    private licenseService: LicenseService,
    private route: ActivatedRoute,
    private ratingService: RatingService,
  ) {

  }

  get modeIsCustomer(): boolean {
    return this.mode === Role.CUSTOMER
  }

  ngOnInit(): void {
    this.route.params.subscribe(q => {
      this.userId = q.userId
      this.route.data.subscribe(data => {
        this.mode = data.mode;
      });
      this.service.getUserDetails(this.userId).subscribe(data => {
        this.user = data;
        if (this.modeIsCustomer && data.role === "WORKER") {
          this.router.navigate([`profile/worker/${this.userId}`]);
        } else if (!this.modeIsCustomer && data.role === "CUSTOMER") {
          this.router.navigate([`profile/customer/${this.userId}`]);
        }
        if (!this.modeIsCustomer) {
          this.licenseService.listCertificate(data.username).subscribe(licenses => {
            this.licenses = licenses;
          })
        }
        this.ratingService.getLatestRatings(this.userId!).subscribe(ratings => {
          this.ratings = ratings;
        });
        this.ratingService.getRatingStats(this.userId!).subscribe(stats => {
          this.ratingStats = stats;
        });
      })
    });
  }

  downloadFile(license: License): void {
    this.licenseService.downloadLicenseFile(license.id).subscribe(
      (blob) => {
        saveAs(blob, license.filename);
      },
      (error) => console.error('Error downloading file', error)
    );
  }
}

