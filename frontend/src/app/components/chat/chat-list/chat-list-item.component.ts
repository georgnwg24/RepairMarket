import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Chat } from '../../../dtos/chat';
import { chatPreviewFormatted } from '../../../utils/date-helper';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { ButtonModule } from "primeng/button";
import {ConfirmationService, MessageService} from "primeng/api";
import {ToastModule} from "primeng/toast";
import {Badge} from "primeng/badge";

@Component({
  selector: 'app-chat-list-item',
  standalone: true,
  imports: [ButtonModule, ToastModule, ConfirmPopupModule, Badge],
  templateUrl: './chat-list-item.component.html',
  styleUrls: ['./chat-list-item.component.scss'],
  providers: [ConfirmationService, MessageService],
})
export class ChatListItemComponent {
  @Input() chat!: Chat;
  @Input() selected = false;
  @Output() selectChat = new EventEmitter<number>();
  @Output() deleteChat = new EventEmitter<number>();

  constructor(private confirmationService: ConfirmationService) {}

  confirmDelete(event: Event) {
    event.stopPropagation();

    this.confirmationService.confirm({
      target: event.target as EventTarget,
      message: `Delete chat with ${this.chat.counterPartName}?`,
      icon: "pi pi-exclamation-triangle",
      rejectButtonProps: {
        label: "Cancel",
        severity: "secondary",
        outlined: true
      },
      acceptButtonProps: {
        label: "Delete",
        severity: "danger",
      },
      accept: () => {
        this.deleteChat.emit(this.chat.id);
      },
    });
  }

  chatPreviewFormatted = chatPreviewFormatted;
}
