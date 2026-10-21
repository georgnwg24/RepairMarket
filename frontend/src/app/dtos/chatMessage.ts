export interface ChatMessage {
  id: number;
  senderUsername: string;
  messageType: MessageType;
  message: string;
  mediaName: string;
  mediaUrl: string;
  read: boolean;
  edited: boolean;
  timestamp: Date;
}

export interface ChatMessagePayload {
  messageType: MessageType;
  message: string;
  mediaName: string;
  mediaUrl: string;
}

export interface ChatImage {
  mediaName: string;
  mediaUrl: string;
}

export enum MessageType {
  TEXT = "TEXT",
  MEDIA = "MEDIA",
}

export interface ChatMessageAction { // edit and delete
  messageId: number;
  newMessage: string; // "" or "something"
  deleted: boolean;
  edited: boolean;
}
