import {fakeAsync, flushMicrotasks} from '@angular/core/testing';
import {BehaviorSubject} from 'rxjs';
import {ChatService} from './chat.service';

describe('Chat connection lifecycle', () => {
  it('disconnects on logout and opens a new connection on the next login', fakeAsync(() => {
    const loggedIn = new BehaviorSubject(false);
    const connect = spyOn<any>(ChatService.prototype, 'connectWebsocket').and.stub();
    const service = new ChatService({} as any, {loggedIn$: loggedIn} as any, {backendUri: '/api/v1'} as any);
    expect(connect).not.toHaveBeenCalled();
    loggedIn.next(true);
    expect(connect).toHaveBeenCalledTimes(1);
    const deactivate = jasmine.createSpy('deactivate').and.returnValue(Promise.resolve());
    (service as any).stompClient = {deactivate};
    loggedIn.next(false);
    expect(deactivate).toHaveBeenCalledTimes(1);
    flushMicrotasks();
    loggedIn.next(true);
    expect(connect).toHaveBeenCalledTimes(2);
    loggedIn.next(false);
  }));

  it('restores an active chat subscription after reconnecting', () => {
    const service = new ChatService({} as any, {loggedIn$: new BehaviorSubject(false)} as any,
      {backendUri: '/api/v1'} as any);
    const unsubscribe = jasmine.createSpy('unsubscribe');
    const subscribe = jasmine.createSpy('subscribe').and.returnValue({unsubscribe});
    const client = {connected: true, subscribe};
    (service as any).stompClient = client;
    const subscription = service.subscribeToChat(42).subscribe();
    (service as any).wsConnected$$.next(true);
    expect(subscribe).toHaveBeenCalledTimes(1);
    client.connected = false;
    (service as any).wsConnected$$.next(false);
    client.connected = true;
    (service as any).wsConnected$$.next(true);
    expect(subscribe).toHaveBeenCalledTimes(2);
    expect(subscribe.calls.mostRecent().args[0]).toBe('/topic/chat/42');
    subscription.unsubscribe();
    expect(unsubscribe).toHaveBeenCalledTimes(1);
  });
});
