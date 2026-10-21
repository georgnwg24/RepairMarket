package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.entity.PushSubscription;

public interface WebPushService {

    /**
     * Sends a push notification to the given subscription via Web Push Protocol with VAPID.
     *
     * @param subscription the {@link PushSubscription} of the target device
     * @param title        the displayed title
     * @param body         the displayed body
     * @param url          the angular router link to be opened on click
     * @param tag          an identifier of the notification type, allowing newer notifications for one topic to replace the old one
     */
    void sendNotification(PushSubscription subscription, String title, String body, String url, String tag);
}
