import { Component, EventEmitter, HostBinding, Input, Output } from '@angular/core';

@Component({
  selector: 'app-confirm-ban-dialog',
  templateUrl: './confirm-ban-dialog.component.html',
  standalone: true,
  styleUrls: ['./confirm-ban-dialog.component.scss']
})
export class ConfirmBanDialogComponent {
  @Input() banWhat = '?';
  @Input() isBanned = false;
  @Output() confirm = new EventEmitter<void>();

  get action(): string {
    return this.isBanned ? 'Unban' : 'Ban';
  }

  @HostBinding('class') cssClass = 'modal fade';
  @HostBinding('attr.data-bs-backdrop') backdrop = 'static';
  @HostBinding('attr.aria-hidden') hidden = 'true';
  @HostBinding('attr.aria-labelledby') labeledBy = 'confirm-ban-dialog-title';

  constructor() {}
}
