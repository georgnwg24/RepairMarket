import {Component, OnInit, ViewChild, AfterViewInit, ChangeDetectorRef, OnDestroy} from '@angular/core';
import {CommonModule, formatDate} from '@angular/common';
import {FormsModule, NgForm, NgModel } from '@angular/forms';
import {JobRequestService} from "../../services/job-request.service";
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {ToastrService} from 'ngx-toastr';
import {JobRequest, JobRequestCreateEdit, JobRequestImage} from "../../dtos/jobRequest";
import {Category} from "../../dtos/category";
import {JobStatus} from "../../dtos/jobStatus";
import {Observable, firstValueFrom} from "rxjs";
import {PropertyService} from "../../services/property.service";
import {Property} from "../../dtos/property";
import { GalleriaModule } from 'primeng/galleria';
import { Galleria } from 'primeng/galleria';

export interface GalleryItem {
  src: string;
  alt: string;
  title?: string;
  id?: number;
  isExisting: boolean;
  originalUrl?: string;
  uniqueId?: string;
  thumbnail?: string;
}

export enum JobRequestCreateEditMode {
  create,
  edit
}

@Component({
  selector: 'app-job-request-edit',
  templateUrl: './job-request-create-edit.component.html',
  styleUrls: ['./job-request-create-edit.component.scss'],
  imports: [
    FormsModule,
    CommonModule,
    GalleriaModule,
    RouterLink
  ],
  standalone: true
})
export class JobRequestCreateEditComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('galleria') galleria: Galleria;
  @ViewChild('form') jobForm: NgForm;
  @ViewChild('jobDeadlineModel') jobDeadlineModel: NgModel;

  jobId: number | undefined;
  mode: JobRequestCreateEditMode = JobRequestCreateEditMode.create;
  jobRequest: JobRequestCreateEdit = {
    title: "",
    category: null,
    deadline: ""
  }
  deadlineIsSet = false;
  jobCategories = Category;
  jobStatus = JobStatus;
  categoryKeys: string[] = [];
  statusKeys: string[] = [];
  properties: Property[] = [];
  error = false;
  errorMessage = '';
  selectedFiles: File[] = [];
  existingImages: JobRequestImage[] = [];
  previewUrls: string[] = [];
  MAX_IMAGES = 5;
  imageCache: { [key: number]: string } = {};
  thumbnailCache: { [key: number]: string } = {};
  processingQueue: boolean = false;
  previewThumbnails: { [originalUrl: string]: string } = {};
  imageErrorMessage: string | null = null;

  // Galleria properties
  galleryItems: GalleryItem[] = [];
  activeIndex: number = 0;
  responsiveOptions = [
    {
      breakpoint: '1024px',
      numVisible: 5
    },
    {
      breakpoint: '768px',
      numVisible: 3
    },
    {
      breakpoint: '560px',
      numVisible: 2
    }
  ];
  protected minDate: string;

  constructor(
    private service: JobRequestService,
    private propertyService: PropertyService,
    private router: Router,
    private notification: ToastrService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.cdr.detectChanges();
      if (this.galleryItems.length > 0) {
        this.refreshGalleria();
      }
    }, 100);
  }

  refreshGalleria(): void {
    const tempItems = [...this.galleryItems];
    this.galleryItems = [];
    this.cdr.detectChanges();

    setTimeout(() => {
      this.galleryItems = tempItems.map(item => ({...item}));
      this.cdr.detectChanges();

      if (this.galleria) {
        try {
          this.galleria.cd.detectChanges();
        } catch (err) {
          console.log('Galleria internal refresh error (non-critical):', err);
        }
      }
    }, 50);
  }

  get modeIsCreate(): boolean {
    return this.mode === JobRequestCreateEditMode.create;
  }

  private get modeActionFinished(): string {
    switch (this.mode) {
      case JobRequestCreateEditMode.create:
        return 'created';
      case JobRequestCreateEditMode.edit:
        return 'changed';
      default:
        return '?';
    }
  }

  async onSubmit(form: NgForm): Promise<void> {
    this.error = false;
    this.imageErrorMessage = null;

    if (!form.valid) {
      Object.keys(form.controls).forEach(key => form.controls[key].markAsTouched());
      return;
    }

    let observable: Observable<JobRequest>;
    switch (this.mode) {
      case JobRequestCreateEditMode.create: observable = this.service.create(this.jobRequest); break;
      case JobRequestCreateEditMode.edit: observable = this.service.update(this.jobRequest, this.jobId); break;
      default: console.error('Unknown JobRequestCreateEditMode', this.mode); return;
    }

    observable.subscribe({
      next: async (data) => {
        this.notification.success(`Job Request ${data.title} successfully ${this.modeActionFinished}.`);

        if (this.selectedFiles.length > 0) {
          try {
            await this.uploadImagesSequentially(data.id);
            await this.router.navigate(['/customer']);
          } catch (error) {
            this.error = true;
            this.errorMessage = `The job request was saved, but some images failed to upload. You can edit the request to try again.`;
          }
        } else {
          await this.router.navigate(['/customer']);
        }
      },
      error: err => {
        if (err.status === 400 && Array.isArray(err.error)) {
          let nonFieldErrors: string[] = [];
          const fieldToControlMap: { [key: string]: string } = {
            title: 'job-title',
            deadline: 'jobDeadline',
            propertyId: 'property',
            category: 'category',
            description: 'description'
          };

          err.error.forEach((validationError: any) => {
            const controlName = fieldToControlMap[validationError.field];
            const control = form.controls[controlName];

            if (control) {
              control.setErrors({ serverError: validationError.defaultMessage });
              control.markAsTouched(); // Ensure the error message is displayed
            } else {
              nonFieldErrors.push(validationError.defaultMessage);
            }
          });

          if (nonFieldErrors.length > 0) {
            this.error = true;
            this.errorMessage = nonFieldErrors.join('<br>');
          }

        } else {
          // Handle other errors (500, network, etc.) in the top banner
          this.error = true;
          this.errorMessage = err.error?.message || err.message || 'An unexpected error occurred.';
        }
      }
    });
  }

  public get deadlineText(): string {
    if (!this.deadlineIsSet || !this.jobRequest.deadline) {
      return '';
    } else {
      return formatDate(this.jobRequest.deadline, 'yyyy-MM-dd', 'en-DK');
    }
  }

  public set deadlineText(date: string) {
    if (date == null || date === '') {
      this.deadlineIsSet = false;
      this.jobRequest.deadline = null;
    } else {
      this.deadlineIsSet = true;
      this.jobRequest.deadline = new Date(date).toISOString().split("T")[0];
    }
  }

  ngOnInit(): void {
    this.route.data.subscribe(data => {
      this.mode = data.mode;
    });

    const today = new Date();
    today.setMinutes(today.getMinutes() - today.getTimezoneOffset());
    this.minDate = today.toISOString().split('T')[0];

    this.categoryKeys = Object.keys(this.jobCategories);
    this.statusKeys = Object.keys(this.jobStatus);

    this.propertyService.listProperties().subscribe({
      next: data => {
        this.properties = data;
        this.jobRequest.propertyId = null;
        this.jobRequest.status = this.jobStatus.pending;

        if (!this.modeIsCreate) {
          this.loadExistingJobRequest();
        } else {
          this.updateGalleryItems();
        }
      },
      error: err => {
        this.error = true;
        this.errorMessage = "Could not load properties from the server. Please try refreshing the page.";
        this.jobForm.form.disable();
      }
    });
  }

  private loadExistingJobRequest(): void {
    if (this.route.snapshot.paramMap.get('id') === null) {
      this.error = true;
      this.errorMessage = 'No Job ID provided in the URL.';
      return;
    }

    this.jobId = Number(this.route.snapshot.paramMap.get('id'));
    if (isNaN(this.jobId)) {
      this.error = true;
      this.errorMessage = 'The Job ID in the URL is not valid.';
      this.jobForm.form.disable();
      return;
    }

    this.service.getById(this.jobId).subscribe({
      next: data => {
        this.jobRequest.title = data.title;
        this.jobRequest.category = data.category;
        this.jobRequest.description = data?.description;
        this.jobRequest.status = data?.status;
        this.jobRequest.deadline = data.deadline;
        this.jobRequest.propertyId = data.propertyId;
        this.deadlineIsSet = true;
        this.loadExistingImages();
      },
      error: err => {
        this.error = true;
        this.errorMessage = `Failed to load the job request (ID: ${this.jobId}). It may have been deleted.`;
        this.jobForm.form.disable();
      }
    });
  }

  private loadExistingImages(): void {
    if (!this.jobId) return;

    this.service.getImages(this.jobId).subscribe({
      next: images => {
        this.existingImages = [...images];
        if (images.length === 0) {
          this.updateGalleryItems();
        } else {
          const imageProcessingPromises = images.map(image => this.loadImageAndGenerateThumbnail(image.id));
          Promise.all(imageProcessingPromises).then(() => {
            this.updateGalleryItems();
            this.refreshGalleria();
          }).catch(err => {
            this.imageErrorMessage = 'Failed to load some images for display. They may be corrupted.';
            this.updateGalleryItems();
          });
        }
      },
      error: err => {
        this.imageErrorMessage = 'Could not load image information from the server.';
      }
    });
  }

  private loadImageAndGenerateThumbnail(imageId: number): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!this.jobId) {
        return reject(new Error('Job ID not set for loading image.'));
      }
      if (this.imageCache[imageId] && this.thumbnailCache[imageId]) {
        return resolve();
      }

      this.service.getImageData(this.jobId, imageId).subscribe({
        next: (blob) => {
          const reader = new FileReader();
          reader.onload = async (e: any) => {
            const fullImageUrl = e.target.result as string;
            this.imageCache[imageId] = fullImageUrl;

            try {
              const thumbnailBlob = await this.createThumbnailFromBlob(blob);
              const thumbnailReader = new FileReader();
              thumbnailReader.onload = () => {
                this.thumbnailCache[imageId] = thumbnailReader.result as string;
                resolve();
              };
              thumbnailReader.onerror = reject;
              thumbnailReader.readAsDataURL(thumbnailBlob);
            } catch (err) {
              console.warn('Failed to create thumbnail for existing image, using full image as thumbnail:', err);
              this.thumbnailCache[imageId] = fullImageUrl;
              resolve();
            }
          };
          reader.onerror = (error) => {
            console.error(`FileReader error for image ID ${imageId}:`, error);
            reject(error);
          };
          reader.readAsDataURL(blob);
        },
        error: (err) => {
          console.error(`Error loading image data for image ID ${imageId}:`, err);
          this.imageCache[imageId] = 'assets/BRP_logo.png';
          this.thumbnailCache[imageId] = 'assets/BRP_logo.png';
          resolve();
        }
      });
    });
  }

  public get submitButtonText(): string {
    switch (this.mode) {
      case JobRequestCreateEditMode.create:
        return 'Create Job Request';
      case JobRequestCreateEditMode.edit:
        return 'Save Changes';
      default:
        return '?';
    }
  }

  public dynamicCssClassesForInput(input: NgModel): any {
    return {
      'is-invalid': !input.valid && (input.dirty || input.touched),
    };
  }

  async onFileSelect(event: any) {
    this.imageErrorMessage = null;
    let warnings: string[] = [];

    const files = event.target.files;
    if (!files || files.length === 0) return;

    try {
      this.processingQueue = true;
      const filesToProcess: File[] = [];
      const newPreviewUrls: string[] = [];
      const newPreviewThumbnails: { [originalUrl: string]: string } = {};

      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        if (this.selectedFiles.length + this.existingImages.length + filesToProcess.length >= this.MAX_IMAGES) {
          warnings.push(`Maximum of ${this.MAX_IMAGES} images allowed. Some files were ignored.`);
          break;
        }

        if (file.type.match(/image\/(jpeg|png)/) && file.size <= 5 * 1024 * 1024) {
          filesToProcess.push(file);
        } else {
          warnings.push(`File "${file.name}" was skipped (must be JPEG/PNG and under 5MB).`);
        }
      }

      for (const file of filesToProcess) {
        try {
          const thumbnailUrl = await this.createThumbnailFromFile(file);
          const previewUrl = await new Promise<string>((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = (e: any) => resolve(e.target.result as string);
            reader.onerror = reject;
            reader.readAsDataURL(file);
          });
          newPreviewUrls.push(previewUrl);
          newPreviewThumbnails[previewUrl] = thumbnailUrl;
          this.selectedFiles = [...this.selectedFiles, file];
        } catch (error) {
          warnings.push(`Failed to process image: ${file.name}`);
        }
      }

      this.previewUrls = [...this.previewUrls, ...newPreviewUrls];
      this.previewThumbnails = { ...this.previewThumbnails, ...newPreviewThumbnails };

      this.updateGalleryItems();
      this.activeIndex = this.galleryItems.length > 0 ? this.galleryItems.length - newPreviewUrls.length : 0;
      this.cdr.detectChanges();
      this.refreshGalleria();

    } finally {
      this.processingQueue = false;
      event.target.value = '';
      if (warnings.length > 0) {
        this.imageErrorMessage = warnings.join('<br>');
      }
    }
  }

  updateGalleryItems() {
    const newItems: GalleryItem[] = [];

    this.existingImages.forEach(image => {
      if (this.imageCache[image.id]) {
        newItems.push({
          src: this.imageCache[image.id],
          alt: 'Existing image',
          id: image.id,
          isExisting: true,
          uniqueId: `existing_${image.id}`,
          thumbnail: this.thumbnailCache[image.id] || this.imageCache[image.id]
        });
      }
    });

    this.previewUrls.forEach((url, index) => {
      const file = this.selectedFiles[index];
      const uniqueFileId = file ? `${file.name}-${file.size}-${file.lastModified}` : `preview_${index}_temp`;
      newItems.push({
        src: url,
        alt: file?.name || 'New image preview',
        title: file?.name || `New image ${index + 1}`,
        isExisting: false,
        originalUrl: url,
        uniqueId: `preview_${uniqueFileId}`,
        thumbnail: this.previewThumbnails[url] || url
      });
    });

    this.galleryItems = newItems.map(item => ({...item}));

    if (this.galleryItems.length === 0) {
      this.activeIndex = 0;
    } else if (this.activeIndex >= this.galleryItems.length) {
      this.activeIndex = this.galleryItems.length - 1;
    }

    this.cdr.detectChanges();
  }

  ngOnDestroy(): void {
    [
      ...Object.values(this.imageCache),
      ...Object.values(this.thumbnailCache),
      ...this.previewUrls,
      ...Object.values(this.previewThumbnails)
    ].forEach(url => {
      if (url && url.startsWith('blob:')) {
        URL.revokeObjectURL(url);
      }
    });
  }

  deleteExistingImage(imageId: number): void {
    if (!this.jobId) return;
    this.imageErrorMessage = null;

    this.service.deleteImage(this.jobId, imageId).subscribe({
      next: () => {
        this.existingImages = this.existingImages.filter(img => img.id !== imageId);
        delete this.imageCache[imageId];
        delete this.thumbnailCache[imageId];
        this.updateGalleryItems();
        setTimeout(() => this.refreshGalleria(), 100);
        this.notification.success('Image deleted successfully!');
      },
      error: (err) => {
        this.imageErrorMessage = 'The image could not be deleted from the server. Please try again.';
      }
    });
  }

  private async uploadImagesSequentially(jobRequestId: number): Promise<void> {
    for (let i = 0; i < this.selectedFiles.length; i++) {
      const file = this.selectedFiles[i];
      try {
        await firstValueFrom(this.service.uploadImage(jobRequestId, file, i));
      } catch (error) {
        console.error(`Failed to upload image ${file.name}:`, error);
        throw error;
      }
    }
  }

  createThumbnailFromFile(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e: any) => {
        const img = new Image();
        img.onload = () => {
          try {
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');

            const MAX_SIZE = 150;
            let width = img.width;
            let height = img.height;

            if (width > height) {
              if (width > MAX_SIZE) {
                height = Math.round(height * (MAX_SIZE / width));
                width = MAX_SIZE;
              }
            } else {
              if (height > MAX_SIZE) {
                width = Math.round(width * (MAX_SIZE / height));
                height = MAX_SIZE;
              }
            }

            canvas.width = MAX_SIZE;
            canvas.height = MAX_SIZE;
            ctx.fillStyle = '#f8f9fa';
            ctx.fillRect(0, 0, MAX_SIZE, MAX_SIZE);

            const x = (MAX_SIZE - width) / 2;
            const y = (MAX_SIZE - height) / 2;

            ctx.drawImage(img, x, y, width, height);

            const dataUrl = canvas.toDataURL('image/jpeg', 0.8);
            resolve(dataUrl);
          } catch (error) {
            console.error('Error creating thumbnail from file:', error);
            resolve(e.target.result);
          }
        };
        img.onerror = (err) => {
          console.error('Image load error for thumbnail creation from file:', err);
          reject(new Error('Failed to load image for thumbnail creation'));
        };
        img.src = e.target.result;
      };
      reader.onerror = (err) => {
        console.error('FileReader error:', err);
        reject(new Error('Failed to read file for thumbnail creation'));
      };
      reader.readAsDataURL(file);
    });
  }

  createThumbnailFromBlob(blob: Blob): Promise<Blob> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      const reader = new FileReader();
      reader.onload = (e: any) => {
        img.src = e.target.result;
      };
      reader.onerror = reject;
      reader.readAsDataURL(blob);

      img.onload = () => {
        try {
          const canvas = document.createElement('canvas');
          const ctx = canvas.getContext('2d');

          const MAX_SIZE = 150;
          let width = img.width;
          let height = img.height;

          if (width > height) {
            if (width > MAX_SIZE) {
              height = Math.round(height * (MAX_SIZE / width));
              width = MAX_SIZE;
            }
          } else {
            if (height > MAX_SIZE) {
              width = Math.round(width * (MAX_SIZE / height));
              height = MAX_SIZE;
            }
          }

          canvas.width = MAX_SIZE;
          canvas.height = MAX_SIZE;
          ctx.fillStyle = '#f8f9fa';
          ctx.fillRect(0, 0, MAX_SIZE, MAX_SIZE);
          const x = (MAX_SIZE - width) / 2;
          const y = (MAX_SIZE - height) / 2;
          ctx.drawImage(img, x, y, width, height);

          canvas.toBlob(thumbnailBlob => {
            if (thumbnailBlob) {
              resolve(thumbnailBlob);
            } else {
              reject(new Error('Canvas to Blob conversion failed.'));
            }
          }, 'image/jpeg', 0.8);

        } catch (error) {
          console.error('Error creating thumbnail from blob:', error);
          reject(error);
        }
      };
      img.onerror = (err) => {
        console.error('Image load error for thumbnail creation from blob:', err);
        reject(new Error('Failed to load image for thumbnail creation from blob'));
      };
    });
  }

  onActiveIndexChange(index: number) {
    this.activeIndex = index;
  }

  removeImage(item: GalleryItem) {
    if (item.isExisting && item.id !== undefined) {
      this.deleteExistingImage(item.id);
    } else if (!item.isExisting && item.originalUrl) {
      delete this.previewThumbnails[item.originalUrl];

      const indexInPreviewUrls = this.previewUrls.indexOf(item.originalUrl);
      if (indexInPreviewUrls > -1) {
        this.previewUrls = this.previewUrls.filter(u => u !== item.originalUrl);
        this.selectedFiles = this.selectedFiles.filter((_, index) => index !== indexInPreviewUrls);

        this.updateGalleryItems();
        if (this.activeIndex >= this.galleryItems.length) {
          this.activeIndex = Math.max(0, this.galleryItems.length - 1);
        }
        this.cdr.detectChanges();
        this.refreshGalleria();
      }
    }
  }

  public get isEditMode(): boolean {
    return this.mode === JobRequestCreateEditMode.edit;
  }
}
