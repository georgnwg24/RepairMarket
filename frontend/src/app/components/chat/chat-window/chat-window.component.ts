import {Component, ElementRef, HostListener, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges, ViewChild} from "@angular/core";
import {ChatMessage, ChatMessagePayload, MessageType} from "../../../dtos/chatMessage";
import {ChatService} from "../../../services/chat.service";
import {ToastrService} from "ngx-toastr";
import {FormsModule} from "@angular/forms";
import {Subscription} from "rxjs";
import {HttpStatusCode} from "@angular/common/http";
import {Router} from "@angular/router";
import {NgIf} from "@angular/common";
import {PdfViewerModule} from "ng2-pdf-viewer";
import {ReportService} from "../../../services/report.service";
import {ChatMessageItemComponent} from "./chat-message-item.component";
import {Dialog} from "primeng/dialog";
import {Textarea} from "primeng/textarea";
import {ConfirmationService} from "primeng/api";
import {Button} from "primeng/button";
import { ConfirmDialogModule } from 'primeng/confirmdialog';

@Component({
  selector: 'app-chat-window',
  templateUrl: './chat-window.component.html',
  imports: [
    FormsModule,
    NgIf,
    PdfViewerModule,
    ChatMessageItemComponent,
    Dialog,
    Textarea,
    ConfirmDialogModule,
    Button,
  ],
  styleUrls: ['./chat-window.component.scss']
})
export class ChatWindowComponent implements OnInit, OnChanges, OnDestroy {
  @Input() chatId!: number;
  @Input() currentUsername: string;

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  messageMaxLength = 4095;
  messages: ChatMessage[] = [];
  imageUrls: Record<number, string> = {}; // prefetching and creating temporary urls to access images (raw didn't work)
  messageText: string  = "";

  showPreview = false;
  previewImageUrl: string | null = null;
  previewPdf: { data: Uint8Array } | null = null;
  private selectedFile: File | null = null;

  private websocketSubs = new Subscription();

  constructor(private router: Router, private chatService: ChatService, private reportService: ReportService, private confirmationService: ConfirmationService, private notification: ToastrService) {}

  ngOnInit(): void {
    this.loadMessagesAndSubscribeToChat(this.chatId);
    document.getElementById("writingBox")?.focus();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['chatId'] && !changes['chatId'].firstChange) {
      this.loadMessagesAndSubscribeToChat(changes['chatId'].currentValue);
    }
  }

  ngOnDestroy() {
    this.websocketSubs.unsubscribe();
    Object.values(this.imageUrls).forEach(URL.revokeObjectURL);
  }

  draggingFile = false;
  private dragCounter = 0;

  // BugFix if user drags back into windows explorer
  @HostListener('window:drop', ['$event'])
  @HostListener('document:dragend', ['$event'])
  onGlobalReset(e: DragEvent) {
    this.dragCounter = 0;
    this.draggingFile = false;
  }

  @HostListener('dragenter', ['$event'])
  onDragEnter(event: DragEvent) {
    event.preventDefault();
    this.dragCounter++;
    if (event.dataTransfer?.types.includes('Files')) {
      this.draggingFile = true;
    }
  }

  @HostListener('dragleave', ['$event'])
  onDragLeave(event: DragEvent) {
    event.preventDefault();
    this.dragCounter--;
    if (this.dragCounter === 0) {
      this.draggingFile = false;
    }
  }

  @HostListener('dragover', ['$event'])
  onDragOver(event: DragEvent) {
    event.preventDefault();
  }

  @HostListener("drop", ['$event'])
  onDragDrop($event: DragEvent) {
    $event.preventDefault();
    this.draggingFile = false;

    const files = $event.dataTransfer?.files;
    if (!files || files.length !== 1) {
      this.notification.error("Only one file at the time!");
      return;
    }
    this.processFile(files[0]);
  }


  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.processFile(input.files[0]);
    input.value = '';
  }

  private processFile(file: File) {
    this.selectedFile = file;
    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = () => {
        this.previewImageUrl = reader.result as string;
        this.previewPdf = null;
        this.showPreview = true;
      };
      reader.readAsDataURL(file);
    } else if (file.type === 'application/pdf') {
      const reader = new FileReader();
      reader.onload = () => {
        this.previewPdf = { data: new Uint8Array(reader.result as ArrayBuffer) };
        this.previewImageUrl = null;
        this.showPreview = true;
      };
      reader.readAsArrayBuffer(file);
    } else {
      this.notification.error('Only Images and PDFs allowed!');
      this.resetPreview();
    }
    this.scrollToBottom();
  }

  cancelPreview() {
    this.resetPreview();
  }

  confirmUpload() {
    if (!this.selectedFile) return;
    this.chatService.uploadImage(this.selectedFile).subscribe({
      next: info => {
        this.chatService.sendWsMessage(this.chatId, {
          messageType: MessageType.MEDIA,
          message: 'Attachment',
          mediaName: info.mediaName,
          mediaUrl: info.mediaUrl
        });
        this.resetPreview();
        this.scrollToBottom(500);
      },
      error: e => {
        if (e.status === HttpStatusCode.PayloadTooLarge) this.notification.error(e.error);
        else if (e.status === HttpStatusCode.UnsupportedMediaType) this.notification.error('Only Images and PDFs allowed!');
        else this.notification.error(e.error);
        this.resetPreview();
      }
    });
  }

  private resetPreview() {
    this.showPreview = false;
    this.previewImageUrl = null;
    this.previewPdf = null;
    this.selectedFile = null;
  }


  private loadMessagesAndSubscribeToChat(chatId: number) {
    this.websocketSubs.unsubscribe(); // else multiple messages
    this.websocketSubs = new Subscription();
    this.imageUrls = {};

    this.chatService.getChatMessages(chatId).subscribe({
      next: messages => {
        this.messages = messages.reverse();
        this.preloadImages(this.messages);
        this.chatService.sendReadMessage(chatId); // mark as read
        this.scrollToBottom();
      },
      error: err => {
        if (err.status === HttpStatusCode.NotFound) {
          this.router.navigate(['/chats']);
          return;
        }
        this.notification.error('Failed to load chatMessages: ', err)
      },
    });

    this.websocketSubs.add(
      this.chatService.subscribeToChat(chatId).subscribe(
      msg => {
        this.messages.push(msg);
        if (msg.messageType === MessageType.MEDIA) {
          this.preloadImage(msg.id, msg.mediaUrl);
        }
        if (msg.senderUsername !== this.currentUsername) {
          this.chatService.sendReadMessage(chatId); // notify that we read the message "live"
        }

        this.scrollToBottom();
      })
    );

    this.websocketSubs.add(
      this.chatService.subscribeToReadMessages(chatId).subscribe(({ reader }) => {
        if (reader !== this.currentUsername) {
          this.messages.forEach(msg => msg.read = true);
        }
      })
    );

    this.websocketSubs.add(
      this.chatService.subscribeToChatDeleted().subscribe(deletedChatId => {
        if (deletedChatId === this.chatId) {
          this.notification.warning("This chat has been deleted by the other user.");
          this.router.navigate(["/chats"]);
        }
      })
    );

    this.websocketSubs.add(
      this.chatService.subscribeToChatMessageActions(chatId).subscribe(messageAction => {
        if (messageAction.deleted) {
          this.messages = this.messages.filter(msg => msg.id !== messageAction.messageId);
        } else if (messageAction.edited) {
          const msg = this.messages.find(m => m.id === messageAction.messageId);
          if (msg) {
            msg.message = messageAction.newMessage;
            msg.edited = true;
          }
        }
      })
    );
  }

  sendMessage(): void {
    document.getElementById("writingBox").focus();

    if (!this.messageText) {
      return;
    }

    this.messageText = this.messageText.trim();

    if (!this.messageText) {
      return;
    }

    if (this.messageText.length > this.messageMaxLength) {
      return;
    }

    const message: ChatMessagePayload = {
      messageType: MessageType.TEXT,
      message: this.messageText,
      mediaName: "",
      mediaUrl: "",
    };

    this.chatService.sendWsMessage(this.chatId, message);

    this.messageText = '';
  }


  openDeleteDialog(messageId: number) {

    const message = this.messages.find(msg => msg.id === messageId);

    this.confirmationService.confirm({
      message: `Delete "${message.message.slice(0,60)}${message.message.length > 60 ? "..." : ""}"?`,
      header: 'Delete Message',
      icon: "pi pi-info-circle",
      rejectLabel: 'Cancel',
      rejectButtonProps: {
        label: 'Cancel',
        severity: 'secondary',
        outlined: true,
      },
      acceptButtonProps: {
        label: "Delete",
        severity: "danger",
      },
      accept: () => {
        this.chatService.sendMessageAction(this.chatId, "DELETE", messageId);
        this.messages = this.messages.filter(msg => msg.id !== messageId);
        this.notification.success("Deleted the message!")
      },
    });
  }

  displayEditDialog = false;
  editingMessageId: number | null = null;
  editMessageText = "";

  openEditDialog(messageId: number) {
    const msg = this.messages.find(m => m.id === messageId);

    if (!msg || msg.messageType !== MessageType.TEXT) return; // shouldn't ever get called

    this.editingMessageId = messageId;
    this.editMessageText = msg.message;
    this.displayEditDialog = true;
  }

  confirmEdit() {
    const newText = this.editMessageText?.trim() || "";
    if (!newText) {
      this.notification.error("Message can not be empty");
      return;
    }
    if (newText.length > this.messageMaxLength) {
      this.notification.error(`Maximum length is ${this.messageMaxLength}!`);
      return;
    }
    this.chatService.sendMessageAction(
      this.chatId,
      "EDIT",
      this.editingMessageId!,
      newText
    );
    this.displayEditDialog = false;
  }

  cancelEdit() {
    this.displayEditDialog = false;
    this.editingMessageId = null;
    this.editMessageText = "";
  }

  displayReportDialog = false;
  reportingMessageId: number | null = null;
  reportMessageText = "";

  openReportDialog(messageId: number) {
    const msg = this.messages.find(m => m.id === messageId);

    this.reportingMessageId = messageId;
    this.reportMessageText = "";
    this.displayReportDialog = true;
  }

  confirmReport() {
    const newText = this.reportMessageText?.trim() || "";
    if (!newText) {
      this.notification.error("Message can not be empty");
      return;
    }
    if (newText.length > this.messageMaxLength) {
      this.notification.error(`Maximum length is ${this.messageMaxLength}!`);
      return;
    }

    this.reportService.reportMessage({
      messageId: this.reportingMessageId,
      chatId: this.chatId,
      reason: this.reportMessageText
    }).subscribe({
      next: value => this.notification.success(`Successfully reported ${value.targetUsername}`),
      error: err => {
        switch (err.status) {
          case HttpStatusCode.Conflict:
            this.notification.warning("You have already reported this message!");
            break;
          case HttpStatusCode.NotFound:
            this.notification.warning("Couldn't find this chatMessage, try reloading the page.")
            break;
          default:
            this.notification.error("Something went wrong");
            console.error(err);
        }
      },
    });

    this.displayReportDialog = false;
  }

  cancelReport() {
    this.displayReportDialog = false;
    this.reportingMessageId = null;
    this.reportMessageText = "";
  }

  onMessageInputKeyDown($event: KeyboardEvent) {
    if ($event.key == "Enter" && !$event.shiftKey) {
      $event.preventDefault();
      this.sendMessage();
    }
  }

  private preloadImages(messages: ChatMessage[]) {
    messages.forEach((msg) => {
      if (msg.messageType === MessageType.MEDIA) {
        this.preloadImage(msg.id, msg.mediaUrl);
      }
    });
  }

  private preloadImage(id: number, mediaUrl: string) {
    this.chatService.getImageUrl(mediaUrl).subscribe({
      next: blobURL => {
        this.imageUrls[id] = blobURL;
      },
      error: err => console.error("Couldn't get blobUrl from image", err.error),
    });
  }


  private scrollToBottom(delay?: number): void {
    if (delay) {
      setTimeout(() => {
        const container = document.querySelector('.chat-messages-container');
        container?.scrollTo({ top: container.scrollHeight, behavior: 'auto' });
      }, delay);
    } else {
      setTimeout(() => {
        const container = document.querySelector('.chat-messages-container');
        container?.scrollTo({ top: container.scrollHeight, behavior: 'auto' });
      });
    }
  }

}
