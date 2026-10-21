import { Component, EventEmitter, Input, Output } from "@angular/core";
import { Chat } from "../../../dtos/chat";
import {ChatListItemComponent} from "./chat-list-item.component";
import {IconField} from "primeng/iconfield";
import {InputIcon} from "primeng/inputicon";
import {InputText} from "primeng/inputtext";
import {FloatLabel} from "primeng/floatlabel";
import {FormsModule} from "@angular/forms";

@Component({
  selector: 'app-chat-list',
  standalone: true,
  templateUrl: './chat-list.component.html',
  styleUrls: ['./chat-list.component.scss'],
  imports: [
    ChatListItemComponent,
    IconField,
    InputIcon,
    InputText,
    FloatLabel,
    FormsModule
  ],
})
export class ChatListComponent {
  @Input() chatsOverview: Chat[] = [];
  @Input() selectedChatId: number = -1;
  @Output() selectChat = new EventEmitter<number>();
  @Output() deleteChat = new EventEmitter<number>();

  currentFilterTerm = '';

  get filteredChats() {
    return this.chatsOverview.filter(c =>
      c.counterPartName.toLowerCase().includes(this.currentFilterTerm) ||
      c.jobRequestTitle.toLowerCase().includes(this.currentFilterTerm) ||
      (c.lastMessageOfCounterpart || '').toLowerCase().includes(this.currentFilterTerm)
    );
  }

}
