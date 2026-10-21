import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { JobRequestSearch } from '../../dtos/jobRequest';
import { Property } from '../../dtos/property';
import { Category } from '../../dtos/category'
import { JobStatus } from '../../dtos/jobStatus'
import {ToastrService} from "ngx-toastr";
import {SearchAlertCreate} from "../../dtos/searchAlert";
import {SearchAlertService} from "../../services/search-alert.service";

type CheckboxMap = { [key: string]: boolean };

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './filter-sidebar.component.html',
  styleUrls: ['./filter-sidebar.component.scss']
})
export class FilterSidebarComponent implements OnInit {
  @Input() mode: 'customer' | 'worker' | 'admin' = 'worker';
  @Input() properties: Property[] = [];
  @Input() initialSearch: JobRequestSearch | null = null;

  @Output() searchChange = new EventEmitter<JobRequestSearch>();

  searchModel: {
    text: string;
    deadlineBefore: string;
    deadlineAfter: string;
    distance: number;
    distanceEnabled: boolean;
    selectedPropertyId: number | null;
    categories: CheckboxMap;
    statuses: CheckboxMap;
    priceMin: number | null;
    priceMax: number | null;
  } = {
    text: '',
    deadlineBefore: '',
    deadlineAfter: '',
    distance: 25,
    distanceEnabled: false,
    selectedPropertyId: null,
    categories: {},
    statuses: {},
    priceMin: null,
    priceMax: null,
  };

  availableCategories = Object.values(Category);
  availableStatuses = Object.values(JobStatus);

  private searchUpdated = new Subject<void>();

  sectionState: { [key: string]: boolean } = {
    text: true, property: true, deadline: true, status: false,
    price: true, distance: true, categories: false,
  };

  constructor(
    private notification: ToastrService,
    private searchAlertService: SearchAlertService,
  ) {
    this.availableCategories.forEach(cat => this.searchModel.categories[cat] = false);
    this.availableStatuses.forEach(stat => this.searchModel.statuses[stat] = false);
  }

  ngOnInit(): void {
    if (this.initialSearch) {
      if (this.initialSearch.title) this.searchModel.text = this.initialSearch.title;

      if (this.initialSearch.category) {
        const categories = this.initialSearch.category.split(',');
        categories.forEach(cat => {
          if (this.searchModel.categories.hasOwnProperty(cat)) {
            this.searchModel.categories[cat] = true;
          }
        });
      }

      if (this.initialSearch.distance !== undefined) {
        this.searchModel.distance = this.initialSearch.distance;
        this.searchModel.distanceEnabled = true;
      }
      this.emitSearchCriteria();
    }

    this.searchUpdated.pipe(
      debounceTime(400)
    ).subscribe(() => {
      this.emitSearchCriteria();
    });
  }

  onModelChange(): void {
    this.searchUpdated.next();
  }

  onDistanceSliderChange(): void {
    if (!this.searchModel.distanceEnabled) {
      this.searchModel.distanceEnabled = true;
    }
    this.onModelChange();
  }

  selectProperty(propertyId: number | null): void {
    if (this.searchModel.selectedPropertyId === propertyId) {
      this.searchModel.selectedPropertyId = null;
    } else {
      this.searchModel.selectedPropertyId = propertyId;
    }
    this.onModelChange();
  }

  toggleSection(section: string): void {
    if (this.sectionState.hasOwnProperty(section)) {
      this.sectionState[section] = !this.sectionState[section];
    }
  }

  private emitSearchCriteria(): void {
    const criteria: JobRequestSearch = {};

    if (this.searchModel.text) criteria.title = this.searchModel.text;

    const selectedCategories = Object.keys(this.searchModel.categories).filter(key => this.searchModel.categories[key]);
    if (selectedCategories.length > 0) criteria.category = selectedCategories.join(',');

    if (this.mode === 'customer') {
      if (this.searchModel.deadlineBefore) criteria.deadline = this.searchModel.deadlineBefore;
      if (this.searchModel.selectedPropertyId) criteria.propertyId = this.searchModel.selectedPropertyId.toString();
      const selectedStatuses = Object.keys(this.searchModel.statuses).filter(key => this.searchModel.statuses[key]);
      if (selectedStatuses.length > 0) criteria.status = selectedStatuses.join(',');
    } else if (this.mode === 'worker') {
      if (this.searchModel.deadlineAfter) criteria.deadline = this.searchModel.deadlineAfter;
      if (this.searchModel.distanceEnabled) {
        criteria.distance = this.searchModel.distance;
      }
      if (this.searchModel.priceMin !== null) criteria.lowestPriceMin = this.searchModel.priceMin;
      if (this.searchModel.priceMax !== null) criteria.lowestPriceMax = this.searchModel.priceMax;
    } else if (this.mode === 'admin') {
      const selectedStatuses = Object.keys(this.searchModel.statuses).filter(key => this.searchModel.statuses[key]);
      if (selectedStatuses.length > 0) criteria.status = selectedStatuses.join(',');
    }

    this.searchChange.emit(criteria);
  }

  saveSearchAlert(): void {
    const dto: SearchAlertCreate = {};

    if (this.searchModel.text) {
      dto.keywords = this.searchModel.text;
    }
    if (this.searchModel.distanceEnabled) {
      dto.maxDistance = this.searchModel.distance;
    }
    const selectedCategories = Object.keys(this.searchModel.categories).filter(key => this.searchModel.categories[key]);
    if (selectedCategories.length > 0) {
      dto.categories = selectedCategories;
    }

    this.searchAlertService.createAlert(dto).subscribe({
      next: () => {
        this.notification.success('Search alert saved successfully.');
      },
      error: (err) => {
        console.error('Failed to save search alert', err);
        this.notification.warning(err.error || 'Failed to save search alert.', 'Error');
      }
    });
  }

}
