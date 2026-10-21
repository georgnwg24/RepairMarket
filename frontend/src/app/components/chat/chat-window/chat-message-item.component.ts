import {Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild} from "@angular/core";
import {ChatMessage, MessageType} from "../../../dtos/chatMessage";
import {chatMessageFormatted} from "../../../utils/date-helper";
import {NgClass} from "@angular/common";
import {Popover, PopoverModule} from "primeng/popover";
import {MenuItem} from "primeng/api";
import {ButtonModule} from "primeng/button";
import {Menu} from "primeng/menu";

@Component({
  selector: 'app-chat-message-item',
  standalone: true,
  templateUrl: './chat-message-item.component.html',
  styleUrls: ['./chat-message-item.component.scss'],
  imports: [
    NgClass,
    PopoverModule,
    ButtonModule,
    Menu
  ],
})
export class ChatMessageItemComponent implements OnInit{
    @Input() msg!: ChatMessage;
    @Input() isMine!: boolean;
    @Input() imageUrl?: string;
    @Output() edit = new EventEmitter<number>();
    @Output() delete = new EventEmitter<number>();
    @Output() report = new EventEmitter<number>();

    @ViewChild('optionsPopover') optionsPopover!: Popover;
    @ViewChild('msgContent', { read: ElementRef }) msgContent!: ElementRef<HTMLElement>;

    actions: MenuItem[] = [];

    private static activePopover: Popover | null = null;

    ngOnInit(): void {
      if (this.isMine && this.msg.messageType == MessageType.MEDIA) {
        this.actions = [
          {
            label: "Delete",
            icon: "pi pi-trash",
            command: () => this.delete.emit(this.msg.id),
          },
        ];
      } else if (this.isMine) {
        this.actions = [
          {
            label: "Edit",
            icon: "pi pi-pencil",
            command: () => this.edit.emit(this.msg.id),
          },
          {
            label: "Delete",
            icon: "pi pi-trash",
            command: () => this.delete.emit(this.msg.id),
          },
        ];
      } else {
        this.actions = [
          {
            label: "Report",
            icon: "pi pi-flag",
            command: () => this.report.emit(this.msg.id),
          },
        ];
      }
    }

  openOptions(event: MouseEvent) {
    if (ChatMessageItemComponent.activePopover && ChatMessageItemComponent.activePopover !== this.optionsPopover) {
      ChatMessageItemComponent.activePopover.hide();
    }

    this.optionsPopover.toggle(event, this.msgContent.nativeElement);

    ChatMessageItemComponent.activePopover = this.optionsPopover;
  }

  onPopoverHide() {
    if (ChatMessageItemComponent.activePopover === this.optionsPopover) {
      ChatMessageItemComponent.activePopover = null;
    }
  }


  protected readonly MessageType = MessageType;
  protected readonly chatMessageFormatted = chatMessageFormatted;
}
