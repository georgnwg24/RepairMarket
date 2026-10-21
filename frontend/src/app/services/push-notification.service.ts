import { Injectable } from '@angular/core';
import {SwPush} from "@angular/service-worker";
import {HttpClient} from "@angular/common/http";
import {environment} from "../../environments/environment";

const baseUri = `${environment.backendUrl}/api/v1/notifications`;

@Injectable({
  providedIn: 'root'
})
export class PushNotificationService {
  readonly VAPID_PUBLIC_KEY = 'BER6kdQUX7-PBTkhVkfG4hVO5u_q43sahUMzmPJb1GIiCs__9rtkQQxBlQ3T0s0sTOAoxb73rp1YuapHNvc9aDg'

  constructor(private swPush: SwPush, private http: HttpClient) { }

  subscribeToNotifications() {
    if (!this.swPush.isEnabled) {
      console.log('Service worker is not enabled, no push notifications will be shown');
      return;
    }

    if (Notification.permission === 'denied') {
      console.log('Notifications are disabled, no push notifications will be shown');
      return;
    }

    this.swPush.requestSubscription({ serverPublicKey: this.VAPID_PUBLIC_KEY })
      .then(sub => {
        const payload = {
          endpoint: sub.endpoint,
          p256dh: sub.toJSON().keys.p256dh,
          auth: sub.toJSON().keys.auth
        }
        this.http.post(baseUri, payload).subscribe();
      })
      .catch(err => console.error('Could not subscribe to notifications', err));
  }

  listenToNotifications() {
    this.swPush.notificationClicks.subscribe(({ notification}) => {

      const url = notification?.data?.url
      if (url) {
        window.location.href = url;
      }
    })
  }
}
