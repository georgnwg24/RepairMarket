import {Injectable} from '@angular/core';
import {filter, first, Observable, ReplaySubject, share, Subject, switchMap} from 'rxjs';
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
    this.connectWebsocket();
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

  subscribeToChat(chatId: number): Observable<ChatMessage> {
    return this.wsConnected$.pipe(
      filter(connected => connected),
      first(),
      switchMap(() =>
        new Observable<ChatMessage>(observer => {
          const sub = this.stompClient.subscribe(
            `/topic/chat/${chatId}`,
            msgFrame => {
              try {
                observer.next(JSON.parse(msgFrame.body));
              } catch (err) {
                observer.error(err);
              }
            }
          );
          return () => sub.unsubscribe();
        })
      ),
      share(),
    );
  }

  subscribeToChatDeleted(): Observable<number> {
    return this.wsConnected$.pipe(
      filter(c => c),
      first(),
      switchMap(() =>
        new Observable<number>(observer => {
          const sub = this.stompClient!
            .subscribe("/user/queue/chat/deleted", frame => {
              const { chatId } = JSON.parse(frame.body);
              observer.next(chatId);
            });
          return () => sub.unsubscribe();
        })
      ),
      share(),
    );
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
    return this.wsConnected$.pipe(
      filter(c => c),
      first(),
      switchMap(() =>
        new Observable<{ reader: string }>(obs => {
          const sub = this.stompClient!.subscribe(
            `/topic/chat/${chatId}/read`,
            frame => {
              try {
                const payload = JSON.parse(frame.body) as { read: boolean; reader: string };
                obs.next({ reader: payload.reader });
              } catch (err) {
                obs.error(err)
              }
            }
          );
          return () => sub.unsubscribe();
        })
      ),
      share(),
    );
  }

  subscribeToChatMessageActions(chatId: number): Observable<ChatMessageAction> {
    return this.wsConnected$.pipe(
      filter(m => m),
      first(),
      switchMap(() =>
        new Observable<ChatMessageAction>(observer => {
          const sub = this.stompClient!.subscribe(
            `/topic/chat/${chatId}/messageAction`,
            frame => {
              try {
                const action = JSON.parse(frame.body) as ChatMessageAction;
                observer.next(action);
              } catch (err) {
                observer.error(err);
              }
            }
          );
          return () => sub.unsubscribe();
        })
      ),
      share(),
    );
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

  private connectWebsocket(): void {
    const rawUrl = "http://localhost:8080/ws-chat";

    this.stompClient = new Stomp.Client({
      webSocketFactory: () => new SockJS(rawUrl) as any,
      reconnectDelay: 1_500,    // try reconnecting if connection cuts or anything happens
      debug: (str) => console.log('[STOMP]', str),
      connectHeaders: {'Authorization': this.auth.getToken()},
      onConnect: (frame) => {
        console.log('[STOMP] Connected:', frame.headers['user-name']);
        this.wsConnected$$.next(true);

        this.stompClient!.subscribe(
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
        this.wsConnected$$.next(false);
      },
    });

    this.stompClient.activate();
  }

}
