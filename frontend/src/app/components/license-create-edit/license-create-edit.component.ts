import {Component, OnInit} from "@angular/core";
import {
  FormsModule, ReactiveFormsModule,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from "@angular/forms";
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {ToastrService} from 'ngx-toastr';
import {Observable} from "rxjs";
import {NgIf} from "@angular/common";
import {License} from "../../dtos/license";
import {LicenseService} from "../../services/license.service"
import { ChangeDetectorRef } from '@angular/core';

export enum LicenseCreateEditMode {
  create,
  edit
}

@Component({
  selector: 'app-properties-create-edit',
  templateUrl: './license-create-edit.component.html',
  standalone: true,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    NgIf,
    RouterLink,
  ],
  styleUrls: ['./license-create-edit.component.scss']
})

export class LicenseCreateEditComponent implements OnInit {
  licenseId: number | undefined;
  licenseForm: UntypedFormGroup;
  selectedFile: File | null = null;
  selectedFileUrl: string | null = null;
  submitted = false;
  error = false;
  errorMessage = '';
  license: License = {
    filename: "",
    description: ""
  };
  mode = LicenseCreateEditMode.create

  constructor(
    private cd: ChangeDetectorRef,
    private formBuilder: UntypedFormBuilder,
    private notification: ToastrService,
    private router: Router,
    private service: LicenseService,
    private route: ActivatedRoute,
  ) {

    this.licenseForm = this.formBuilder.group({
      filename: ['', [Validators.required, Validators.maxLength(255)]],
      description: ['', Validators.maxLength(4095)]
    });
  }

  isDropZoneHovered = false;

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) {
      this.setFile(input.files[0]);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDropZoneHovered = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDropZoneHovered = false;
  }

  onFileDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDropZoneHovered = false;
    if (event.dataTransfer?.files?.length) {
      this.setFile(event.dataTransfer.files[0]);
    }
  }

  setFile(file: File): void {
    this.selectedFile = file;

    this.licenseForm.patchValue({
      filename: file.name
    })

    if (this.selectedFileUrl) {
      URL.revokeObjectURL(this.selectedFileUrl);
    }

    this.selectedFileUrl = URL.createObjectURL(file);
    this.cd.detectChanges();
  }

  removeSelectedFile(): void {
    if (this.selectedFileUrl) {
      URL.revokeObjectURL(this.selectedFileUrl);
    }

    this.selectedFile = null;
    this.selectedFileUrl = null;
    this.licenseForm.patchValue({ filename: '' });
  }

  get modeIsCreate(): boolean {
    return this.mode === LicenseCreateEditMode.create
  }

  ngOnInit(): void {
    this.route.data.subscribe(data => {
      this.mode = data.mode;
    });
    if (!this.modeIsCreate) {
      this.licenseId = Number(this.route.snapshot.paramMap.get('id'))
      this.service.getById(this.licenseId).subscribe(data => {
        if (data.status == 'APPROVED'){
          this.router.navigate([`worker/edit`]);
        }
        this.license = data;
        this.licenseForm.patchValue({
          filename: data.filename,
          description: data.description
        })
        this.service.downloadLicenseFile(this.licenseId).subscribe(data => {
          const fileName = this.license.filename;
          const file = new File([data], fileName, {type: data.type});
          this.setFile(file);

        })
      })

    }
  }

  get form() {
    return this.licenseForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = false;
    if (this.licenseForm.invalid || !this.selectedFile) {
      if (!this.selectedFile) {
        this.error = true;
        this.errorMessage = 'Please select a file to upload.';
      }
      return;
    }
    Object.assign(this.license, this.licenseForm.value);
    const formData = new FormData();
    formData.append('certificate', this.selectedFile);
    formData.append('certificateInfo', new Blob(
      [JSON.stringify(this.license)],
      {type: 'application/json'}
    ));
    let observable: Observable<License>;
    if (this.modeIsCreate) {
      observable = this.service.create(formData)
    } else {
      observable = this.service.update(formData, this.licenseId)
    }
    observable.subscribe({
      next: () => {
        if (this.modeIsCreate) {
          this.notification.success(`successfully added license!`, '', {
            timeOut: 3000,
            closeButton: true,
          });
        } else {
          this.notification.success(`successfully saved changes!`, '', {
            timeOut: 3000,
            closeButton: true,
          });
        }
        this.router.navigate([`worker/edit`]);
      },
      error: err => {
        this.error = true;

        const message = Array.isArray(err.error)
          ? err.error.map((e: any) => e.defaultMessage || JSON.stringify(e)).join('<br>')
          : err.error?.message || err.message || 'Unexpected error';

        this.notification.error(message, 'License Upload Failed', {
          enableHtml: true,
          timeOut: 10000,
        });

        this.errorMessage = message;
      }


    });

  }

  public get submitButtonText(): string {
    switch (this.mode) {
      case LicenseCreateEditMode.create:
        return 'Add License';
      case LicenseCreateEditMode.edit:
        return 'Save Changes';
      default:
        return '?';
    }
  }
}
