import {Injectable} from '@angular/core';
import {distinctUntilChanged, EMPTY, Observable, ReplaySubject, share, Subject, switchMap} from 'rxjs';
import {HttpClient, HttpResponse} from '@angular/common/http';
import * as Stomp from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {Globals} from '../global/globals';
import {Chat, CreatedChat} from "../dtos/chat";
import {ChatImage, ChatMessage, ChatMessageAction, ChatMessagePayload} from "../dtos/chatMessage";
import {AuthService} from "./auth.service";
import {map} from "rxjs/operators";
import {environment} from "../../environments/environment";
import { ChatNotificationDto } from '../dtos/chatNotification';

@Injectable({
  providedIn: 'root'
})
export class ChatService {

  private baseUri: string = this.globals.backendUri + "/chats";

  private stompClient: Stomp.Client | null = null;

  private wsConnected$$ = new ReplaySubject<boolean>(1);
  public wsConnected$ = this.wsConnected$$.asObservable();

  private incomingMessage$$ = new Subject<ChatMessage>();
  public incomingMessages$ = this.incomingMessage$$.asObservable().pipe(share());

  private notification$$ = new Subject<ChatNotificationDto>();
  public notifications$ = this.notification$$.asObservable().pipe(share());

  constructor(private http: HttpClient, private auth: AuthService, private globals: Globals) {
    this.auth.loggedIn$.pipe(distinctUntilChanged()).subscribe(loggedIn => {
      void this.restartConnection(loggedIn);
    });
  }

  engageConversation(jobRequestId: number): Observable<CreatedChat> {
    return this.http.post<CreatedChat>(`${this.baseUri}/engage/${jobRequestId}`, {});
  }

  getChatMessages(chatId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.baseUri}/${chatId}/messages`);
  }

  getListOfChats(): Observable<Chat[]> {
    return this.http.get<Chat[]>(`${this.baseUri}`);
  }

  uploadImage(file: File): Observable<ChatImage> {
    const form = new FormData();
    form.append("image", file, file.name);

    return this.http.post<ChatImage>(`${this.baseUri}/uploads`, form);
  }

  getImageUrl(pathToResource: string): Observable<string> {
    return this.http.get(environment.backendUrl + pathToResource, { responseType: "blob" })
      .pipe(
        map(blob => URL.createObjectURL(blob))
      );
  }

  getLastMessage(chatId: number): Observable<ChatMessage> {
    return this.http.get<ChatMessage>(`${this.baseUri}/${chatId}/messages/last`);
  }

  deleteChat(chatId: number): Observable<HttpResponse<void>> {
    return this.http.delete<void>(`${this.baseUri}/${chatId}`, { observe: "response" });
  }

  // Websocket

  private subscribeToDestination<T>(destination: string): Observable<T> {
    return this.wsConnected$.pipe(
      switchMap(connected => {
        const client = this.stompClient;
        if (!connected || !client?.connected) return EMPTY;
        return new Observable<T>(observer => {
          const sub = client.subscribe(destination, frame => {
            try {
              observer.next(JSON.parse(frame.body));
            } catch (error) {
              observer.error(error);
            }
          });
          return () => {
            if (client.connected) sub.unsubscribe();
          };
        });
      }),
      share()
    );
  }

  subscribeToChat(chatId: number): Observable<ChatMessage> {
    return this.subscribeToDestination<ChatMessage>(`/topic/chat/${chatId}`);
  }

  subscribeToChatDeleted(): Observable<number> {
    return this.subscribeToDestination<{chatId: number}>('/user/queue/chat/deleted').pipe(map(event => event.chatId));
  }

  sendWsMessage(chatId: number, payload: ChatMessagePayload) {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: `/app/chat/${chatId}/sendMessage`,
        body: JSON.stringify(payload),
      });
    } else {
      console.warn("Cannot send WS message, try reloading page.");
    }
  }

  subscribeToReadMessages(chatId: number): Observable<{ reader: string }> {
    return this.subscribeToDestination<{reader: string}>(`/topic/chat/${chatId}/read`);
  }

  subscribeToChatMessageActions(chatId: number): Observable<ChatMessageAction> {
    return this.subscribeToDestination<ChatMessageAction>(`/topic/chat/${chatId}/messageAction`);
  }

  sendReadMessage(chatId: number) {
    const payload = { chatId };
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient?.publish({
        destination: `/app/chat/${chatId}/read`,
        body: JSON.stringify(payload),
      });
    } else {
      console.warn('WS not connected, cannot send readMessage');
    }
  }

  sendMessageAction(chatId: number, action: 'EDIT' | 'DELETE', messageId: number, newMessage?: string) {
    const payload = { action, messageId, newMessage };
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: `/app/chat/${chatId}/messageAction`,
        body: JSON.stringify(payload),
      });
    } else {
      console.warn('WS not connected, cannot send messageAction');
    }
  }

  private connectionGeneration = 0;

  private async restartConnection(loggedIn: boolean): Promise<void> {
    const generation = ++this.connectionGeneration;
    this.wsConnected$$.next(false);
    const previous = this.stompClient;
    this.stompClient = null;
    if (previous) await previous.deactivate();
    if (loggedIn && generation === this.connectionGeneration) this.connectWebsocket();
  }

  private connectWebsocket(): void {
    const rawUrl = `${environment.backendUrl}/ws-chat`;

    const client = new Stomp.Client({
      webSocketFactory: () => new SockJS(rawUrl) as any,
      reconnectDelay: 1_500,    // try reconnecting if connection cuts or anything happens
      debug: (str) => console.log('[STOMP]', str),
      beforeConnect: () => {
        client.connectHeaders = {Authorization: this.auth.getToken()};
      },
      onConnect: (frame) => {
        if (this.stompClient !== client) return;
        console.log('[STOMP] Connected:', frame.headers['user-name']);
        this.wsConnected$$.next(true);

        client.subscribe(
          "/user/queue/notifications",
          (message) => {
            try {
              const dto: ChatNotificationDto = JSON.parse(message.body);
              this.notification$$.next(dto);
            } catch (err) {
              console.error('Failed to parse notification', err);
            }
          });

      },
      onStompError: (frame) => {
        console.error('[STOMP] Broker error:', frame.headers['message']);
      },
      onWebSocketClose: (evt) => {
        console.warn('[STOMP] WebSocket closed.', evt);
        if (this.stompClient === client) this.wsConnected$$.next(false);
      },
    });

    this.stompClient = client;
    client.activate();
  }

}
