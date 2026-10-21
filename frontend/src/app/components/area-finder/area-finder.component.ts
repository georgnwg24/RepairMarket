import { Component, Input } from '@angular/core';
import { UntypedFormGroup, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { PropertyService } from '../../services/property.service';

@Component({
  selector: 'app-area-finder',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './area-finder.component.html',
  styleUrls: ['./area-finder.component.scss']
})
export class AreaFinderComponent {
  @Input() parentForm!: UntypedFormGroup;

  isSelectableCountry = false;
  isAreaLocked = false;
  areaOptions: string[] = [];
  noAreaFoundError = false;

  selectableCountries = [
    'AD', 'AE', 'AI', 'AL', 'AR', 'AS', 'AT', 'AU', 'AX', 'AZ', 'BD', 'BE', 'BG', 'BM', 'BR', 'BY',
    'CA', 'CC', 'CH', 'CL', 'CN', 'CO', 'CR', 'CX', 'CY', 'CZ', 'DE', 'DK', 'DO', 'DZ', 'EC',
    'EE', 'ES', 'FI', 'FK', 'FM', 'FO', 'FR', 'GB', 'GF', 'GG', 'GI', 'GL', 'GP', 'GS', 'GT',
    'GU', 'HK', 'HM', 'HN', 'HR', 'HT', 'HU', 'ID', 'IE', 'IM', 'IN', 'IO', 'IS', 'IT', 'JE',
    'JP', 'KE', 'KR', 'LI', 'LK', 'LT', 'LU', 'LV', 'MA', 'MC', 'MD', 'MH', 'MK', 'MO', 'MP',
    'MQ', 'MT', 'MW', 'MX', 'MY', 'NC', 'NF', 'NL', 'NO', 'NR', 'NU', 'NZ', 'PA', 'PE', 'PF',
    'PH', 'PK', 'PL', 'PM', 'PN', 'PR', 'PT', 'PW', 'RE', 'RO', 'RS', 'RU', 'SE', 'SG', 'SI',
    'SJ', 'SK', 'SM', 'TC', 'TH', 'TR', 'UA', 'US', 'UY', 'VA', 'VI', 'WF', 'WS', 'YT', 'ZA', 'OTH'
  ];
  countryMap: { [code: string]: string } = {
    'AD': 'Andorra', 'AE': 'United Arab Emirates', 'AI': 'Anguilla', 'AL': 'Albania', 'AR': 'Argentina',
    'AS': 'American Samoa', 'AT': 'Austria', 'AU': 'Australia', 'AX': 'Åland Islands', 'AZ': 'Azerbaijan',
    'BD': 'Bangladesh', 'BE': 'Belgium', 'BG': 'Bulgaria', 'BM': 'Bermuda', 'BR': 'Brazil', 'BY': 'Belarus',
    'CA': 'Canada', 'CC': 'Cocos (Keeling) Islands', 'CH': 'Switzerland', 'CL': 'Chile', 'CN': 'China',
    'CO': 'Colombia', 'CR': 'Costa Rica', 'CX': 'Christmas Island', 'CY': 'Cyprus', 'CZ': 'Czechia',
    'DE': 'Germany', 'DK': 'Denmark', 'DO': 'Dominican Republic', 'DZ': 'Algeria', 'EC': 'Ecuador',
    'EE': 'Estonia', 'ES': 'Spain', 'FI': 'Finland', 'FK': 'Falkland Islands', 'FM': 'Micronesia',
    'FO': 'Faroe Islands', 'FR': 'France', 'GB': 'United Kingdom', 'GF': 'French Guiana', 'GG': 'Guernsey',
    'GI': 'Gibraltar', 'GL': 'Greenland', 'GP': 'Guadeloupe', 'GS': 'South Georgia & South Sandwich Islands',
    'GT': 'Guatemala', 'GU': 'Guam', 'HK': 'Hong Kong SAR China', 'HM': 'Heard & McDonald Islands', 'HN': 'Honduras',
    'HR': 'Croatia', 'HT': 'Haiti', 'HU': 'Hungary', 'ID': 'Indonesia', 'IE': 'Ireland', 'IM': 'Isle of Man',
    'IN': 'India', 'IO': 'British Indian Ocean Territory', 'IS': 'Iceland', 'IT': 'Italy', 'JE': 'Jersey',
    'JP': 'Japan', 'KE': 'Kenya', 'KR': 'South Korea', 'LI': 'Liechtenstein', 'LK': 'Sri Lanka', 'LT': 'Lithuania',
    'LU': 'Luxembourg', 'LV': 'Latvia', 'MA': 'Morocco', 'MC': 'Monaco', 'MD': 'Moldova', 'MH': 'Marshall Islands',
    'MK': 'North Macedonia', 'MO': 'Macao SAR China', 'MP': 'Northern Mariana Islands', 'MQ': 'Martinique',
    'MT': 'Malta', 'MW': 'Malawi', 'MX': 'Mexico', 'MY': 'Malaysia', 'NC': 'New Caledonia', 'NF': 'Norfolk Island',
    'NL': 'Netherlands', 'NO': 'Norway', 'NR': 'Nauru', 'NU': 'Niue', 'NZ': 'New Zealand', 'PA': 'Panama', 'PE': 'Peru',
    'PF': 'French Polynesia', 'PH': 'Philippines', 'PK': 'Pakistan', 'PL': 'Poland', 'PM': 'St. Pierre & Miquelon',
    'PN': 'Pitcairn Islands', 'PR': 'Puerto Rico', 'PT': 'Portugal', 'PW': 'Palau', 'RE': 'Réunion',
    'RO': 'Romania', 'RS': 'Serbia', 'RU': 'Russia', 'SE': 'Sweden', 'SG': 'Singapore', 'SI': 'Slovenia',
    'SJ': 'Svalbard & Jan Mayen', 'SK': 'Slovakia', 'SM': 'San Marino', 'TC': 'Turks & Caicos Islands',
    'TH': 'Thailand', 'TR': 'Turkey', 'UA': 'Ukraine', 'US': 'United States', 'UY': 'Uruguay',
    'VA': 'Vatican City', 'VI': 'U.S. Virgin Islands', 'WF': 'Wallis & Futuna', 'WS': 'Samoa',
    'YT': 'Mayotte', 'ZA': 'South Africa', 'OTH': 'Other'
  };

  constructor(
    private propertyService: PropertyService,
  ) { }

  /**
   * This public method is intended to be called by the parent component
   * AFTER the parent has finished loading async data and patching the form.
   * This solves the race condition.
   */
  public triggerInitialStateCheck(): void {
    const countryControl = this.parentForm.get('countryCode');
    const postalControl = this.parentForm.get('postalCode');

    if (countryControl?.value) {
      postalControl?.enable();
      this.onCountryChange(false);
    } else {
      postalControl?.disable();
    }
  }

  onCountryChange(clearFields = true): void {
    const selectedCountryCode = this.parentForm.get('countryCode')?.value;
    const postalControl = this.parentForm.get('postalCode');
    this.isSelectableCountry = this.selectableCountries.includes(selectedCountryCode) && selectedCountryCode !== 'OTH';

    this.noAreaFoundError = false;

    if (selectedCountryCode) {
      postalControl?.enable();
    } else {
      postalControl?.disable();
    }

    if (clearFields) {
      this.parentForm.patchValue({
        postalCode: '',
        area: ''
      });
    }

    this.areaOptions = [];
    this.isAreaLocked = false;

    if (!this.isSelectableCountry) {
      this.parentForm.get('area')?.enable();
    } else {
      this.parentForm.get('area')?.disable();
      if(this.parentForm.get('postalCode')?.value){
        this.onPostalCodeChange();
      }
    }
  }

  onPostalCodeChange(): void {
    const postalCode = this.parentForm.get('postalCode')?.value;
    const countryCode = this.parentForm.get('countryCode')?.value;

    const currentDbArea = this.parentForm.get('area')?.value;

    this.areaOptions = [];
    this.isAreaLocked = false;
    this.noAreaFoundError = false;

    if (!postalCode || !countryCode || !this.isSelectableCountry) {
      if (this.isSelectableCountry) {
        this.parentForm.get('area')?.disable();
      }
      return;
    }

    this.parentForm.get('area')?.disable();

    this.propertyService.lookupArea(postalCode, countryCode).subscribe({
      next: (response) => {
        const areas = response.areaNames;
        this.areaOptions = areas;
        this.parentForm.get('area')?.enable();

        if (areas.length === 0) {
          // Case: no results found
          this.noAreaFoundError = true;
          this.isAreaLocked = false;
          this.areaOptions = [];
          // enable the area field for manual input
        } else if (areas.length === 1) {
          // Case: exactly one result
          this.areaOptions = areas;
          this.isAreaLocked = true;
          this.parentForm.get('area')?.setValue(areas[0]);
        } else {
          // Case: multiple results
          this.areaOptions = areas;
          this.isAreaLocked = false;
          // set a default value if the current one isn't valid
          if (!currentDbArea || !areas.includes(currentDbArea)) {
            this.parentForm.get('area')?.setValue(areas[0]);
          }
        }
      },
      error: () => {
        this.isAreaLocked = false;
        this.parentForm.get('area')?.enable();
      }
    });
  }
}
