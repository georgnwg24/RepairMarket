import { Component, EventEmitter, HostBinding, Input, Output } from '@angular/core';

@Component({
  selector: 'app-confirm-withdraw-dialog',
  standalone: true,
  templateUrl: './confirm-withdraw-dialog.component.html',
  styleUrl: './confirm-withdraw-dialog.component.scss'
})
export class ConfirmWithdrawDialogComponent {
  @Input() withdrawWhat = '?';
  @Output() confirm = new EventEmitter<void>();

  @HostBinding('class') cssClass = 'modal fade';
  @HostBinding('attr.data-bs-backdrop') backdrop = 'static';
  @HostBinding('attr.aria-hidden') hidden = 'true';
  @HostBinding('attr.aria-labelledby') labeledBy = 'confirm-withdraw-dialog-title';

  constructor() {
  }

}
