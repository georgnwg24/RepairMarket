import {Component, HostListener, Input, OnDestroy, OnInit} from "@angular/core";
import { Chat } from "src/app/dtos/chat";
import {ChatService} from "../../services/chat.service";
import {ToastrService} from "ngx-toastr";
import {ChatListComponent} from "./chat-list/chat-list.component";
import {ChatWindowComponent} from "./chat-window/chat-window.component";
import {CommonModule} from "@angular/common";
import {AuthService} from "../../services/auth.service";
import {ActivatedRoute, Router} from "@angular/router";
import {Button} from "primeng/button";
import {HttpStatusCode} from "@angular/common/http";
import {Subscription} from "rxjs";
import {ChatNotificationDto} from "../../dtos/chatNotification";

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [
    CommonModule,
    ChatListComponent,
    ChatWindowComponent,
    Button
  ],
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.scss']
})
export class ChatComponent implements OnInit, OnDestroy {
  chatsOverview: Chat[] = [];
  selectedChatId: number | null = null;
  selectedChatBanned: boolean | null = null;
  currentUsername: string = this.authService.getUsername();
  isMobile = false;

  private notifSub = new Subscription();

  constructor(private route: ActivatedRoute, private chatService: ChatService, private authService: AuthService, private notification: ToastrService) {
    this.updateIsMobile();
  }

  ngOnInit() {
    this.loadChatOverview();

    this.notifSub.add(
      this.chatService.notifications$.subscribe(n => this.onIncomingMessage(n))
    );
  }


  ngOnDestroy() {
    this.notifSub.unsubscribe();
  }

  private onIncomingMessage(n: ChatNotificationDto) {
    // find and remove the chat
    const idx = this.chatsOverview.findIndex(c => c.id === n.chatId);
    if (idx === -1) return;

    const [chat] = this.chatsOverview.splice(idx, 1);

    chat.lastMessageOfCounterpart = n.message;
    chat.lastMessageOfCounterpartTime = new Date();

    if (this.selectedChatId !== n.chatId) {
      chat.numberOfUnreadMessages = (chat.numberOfUnreadMessages || 0) + 1;
    }

    // ABOVE EVERYTHING
    this.chatsOverview.unshift(chat);
  }

  @HostListener('window:resize')
  updateIsMobile() {
    this.isMobile = window.innerWidth <= 800;
  }

  loadChatOverview(): void {
    this.chatService.getListOfChats().subscribe({
      next: value => {
        this.chatsOverview = value;

        const id = this.route.snapshot.paramMap.get("chatId");
        if (id) {
          this.selectedChatId = +id;
          const chat = this.chatsOverview.find(chat => chat.id === +id);
          chat.numberOfUnreadMessages = 0;
        }
      },
      error: err => {
        console.error("Couldnt load chats", err, err.message);
        this.notification.error("Couldnt load chats", err);
      }
    });
  }

  onChatSelected(chatId: number): void {
    this.selectedChatId = chatId;
    const chat = this.chatsOverview.find(chat => chat.id === chatId);
    chat.numberOfUnreadMessages = 0;
    this.selectedChatBanned = chat.counterPartBanned;
  }

  onChatDeleted(chatId: number): void {
    const chatIndex = this.chatsOverview.findIndex(chat => chat.id === chatId);
    if (chatIndex === -1) return;

    const [chat] = this.chatsOverview.splice(chatIndex, 1);

    if (this.selectedChatId === chat.id) {
      this.selectedChatId = null;
      this.selectedChatBanned = null;
    }


    this.chatService.deleteChat(chatId).subscribe({
      next: value => {
        if (value.status === HttpStatusCode.NoContent) {
          this.notification.success(`Successfully deleted ${chat.jobRequestTitle} with ${chat.counterPartName}`);
        } else if (value.status === HttpStatusCode.NotFound) {
          this.notification.warning(`Couldn't find chat ${chat.jobRequestTitle}`);
        }
      },
      error: err => this.notification.error(err),
    });
  }

  getTitle(): string {
    const chat = this.chatsOverview.find(chat => chat.id === this.selectedChatId);
    if (!chat) {
      return "";
    }
    return chat.jobRequestTitle;
  }

  goBack() {
    this.selectedChatId = null;
    this.selectedChatBanned = null;
  }
}
