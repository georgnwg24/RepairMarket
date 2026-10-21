import { Component, EventEmitter, HostBinding, Input, Output } from '@angular/core';
import {FormsModule} from "@angular/forms";

@Component({
  selector: 'app-report-dialog',
  templateUrl: './report-dialog.component.html',
  standalone: true,
  imports: [
    FormsModule
  ],
  styleUrls: ['./report-dialog.component.scss']
})
export class ReportDialogComponent {
  @Input() reportWhat: string = '';
  @Output() confirm = new EventEmitter<string>();

  reason: string = '';

  @HostBinding('class') cssClass = 'modal fade';
  @HostBinding('attr.data-bs-backdrop') backdrop = 'static';
  @HostBinding('attr.aria-hidden') hidden = 'true';
  @HostBinding('attr.aria-labelledby') labeledBy = 'report-dialog-title';

  onConfirm(): void {
    if (this.reason.trim()) {
      this.confirm.emit(this.reason.trim());
      this.reason = '';
    }
  }

  onCancel(): void {
    this.reason = '';
  }
}
