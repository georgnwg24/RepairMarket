export interface Chat {
  id: number;
  jobRequestId: number;
  jobRequestTitle: string;
  counterPartName: string;
  counterPartBanned: boolean;
  lastMessageOfCounterpart?: string;
  lastMessageOfCounterpartTime?: Date;
  numberOfUnreadMessages: number;
}

export interface CreatedChat {
  chatId: number;
}
