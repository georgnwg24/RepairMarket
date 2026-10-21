import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import {Subscription, filter, take} from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import {ChatNotificationDto} from "../../dtos/chatNotification";
import {ChatService} from "../../services/chat.service";

@Component({
  selector: 'app-chat-notifications',
  template: '',
  standalone: true
})
export class ChatNotificationsComponent implements OnInit, OnDestroy {
  private notificationSub  = new Subscription();
  private onChatPage = false;

  constructor(
    private chatService: ChatService,
    private toastr: ToastrService,
    private router: Router
  ) {}

  ngOnInit() {
    // if already on /chats, no notifications, chat-list update is enough
    this.notificationSub.add(
      this.router.events
        .pipe(filter(e => e instanceof NavigationEnd))
        .subscribe((e: NavigationEnd) => {
          this.onChatPage = e.urlAfterRedirects.includes('/chats');
        })
    );

    this.notificationSub.add(
      this.chatService.notifications$.subscribe((n: ChatNotificationDto) => {
        if (!this.onChatPage) {
          this.toastr.info(n.message, `${n.username} sent you a message!`, {
            positionClass: 'toast-bottom-right'
          }).onTap
            .pipe(take(1))
            .subscribe(() => this.router.navigate([`/chats/${n.chatId}`]));
        }
      })
    );
  }

  ngOnDestroy() {
    this.notificationSub.unsubscribe();
  }
}
