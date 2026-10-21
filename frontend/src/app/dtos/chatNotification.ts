import {MessageType} from "./chatMessage";

export interface ChatNotificationDto {
  chatId: number,
  username: string,
  messageType: MessageType,
  message: string,
}
