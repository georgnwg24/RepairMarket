import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { SearchAlertService } from '../../services/search-alert.service';
import { SearchAlertDetail } from '../../dtos/searchAlert';
import {ToastrService} from "ngx-toastr";
import {MatSlideToggle} from "@angular/material/slide-toggle";

@Component({
  selector: 'app-saved-searches',
  standalone: true,
  templateUrl: './saved-searches.component.html',
  styleUrl: './saved-searches.component.scss',
  imports: [CommonModule, FormsModule, MatSlideToggle],
})
export class SavedSearchesComponent implements OnInit {
  searchAlerts: SearchAlertDetail[] = [];
  loading = false;
  error = '';

  constructor(
    private searchAlertService: SearchAlertService,
    private router: Router,
    private notification: ToastrService,
  ) {}

  ngOnInit(): void {
    this.loadSearchAlerts();
  }

  loadSearchAlerts(): void {
    this.loading = true;
    this.searchAlertService.getUserAlerts().subscribe({
      next: (alerts) => {
        this.searchAlerts = alerts;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load search alerts', err);
        this.notification.error(err.error?.message || 'Failed to load search alerts.', 'Error');
        this.loading = false;
      },
    });
  }

  toggleAlert(alert: SearchAlertDetail): void {
    const newStatus = !alert.active;

    this.searchAlertService.updateAlertStatus(alert.id, newStatus).subscribe({
      next: () => {
        this.loadSearchAlerts();
      },
      error: (err) => {
        console.error('Failed to toggle search alert', err);
        this.notification.error(err.error?.message || 'Failed to toggle search alert.', 'Error');
      }
    });
  }

  deleteAlert(id: number): void {
    this.searchAlertService.deleteAlert(id).subscribe({
      next: () => {
        this.notification.success('Search alert deleted successfully.');
        this.loadSearchAlerts();
      },
      error: (err) => {
        console.error('Failed to delete search alert', err);
        this.notification.error(err.error?.message || 'Failed to delete search alert.', 'Error');
      }
    });
  }

  openSearch(alert: SearchAlertDetail): void {

    this.searchAlertService.resetAlertCount(alert.id).subscribe({
      next: () => {
        this.router.navigate(['/worker/requests'], {
          queryParams: {
            keywords: alert.keywords || null,
            maxDistance: alert.maxDistance || null,
            categories: alert.categories?.join(',') || null
          }
        });
      },
      error: (err) => {
        console.error('Failed to delete search alert', err);
        this.notification.error(err.error?.message || 'Failed to open search alert.', 'Error');
      }
    });
  }
}
