package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.entity.PushSubscription;
import at.ac.tuwien.sepr.groupphase.backend.service.WebPushService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.security.Security;
import java.util.Base64;

@Service
public class WebPushServiceImpl implements WebPushService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final PushService pushService;
    private final String vapidSubject = "mailto: <brp@example.com>";
    private final String vapidPublicKey = "BER6kdQUX7-PBTkhVkfG4hVO5u_q43sahUMzmPJb1GIiCs__9rtkQQxBlQ3T0s0sTOAoxb73rp1YuapHNvc9aDg";
    private final String vapidPrivateKey = "A4Z_iiEEnE6MMFGGk0MLsIXiH7jndlK5WJTENlxXKbE";
    private final ObjectMapper objectMapper;

    public WebPushServiceImpl() {
        Security.addProvider(new BouncyCastleProvider());

        try {
            pushService = new PushService();
            pushService.setSubject(vapidSubject);
            pushService.setPublicKey(Utils.loadPublicKey(vapidPublicKey));
            pushService.setPrivateKey(Utils.loadPrivateKey(vapidPrivateKey));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        objectMapper = new ObjectMapper();
    }

    @Override
    public void sendNotification(PushSubscription subscription, String title, String body, String url, String tag) {
        LOGGER.trace("called sendNotification() with parameters: {}, {}, {}, {}", subscription, title, body, url);

        String json;

        ObjectNode data = objectMapper.createObjectNode();
        data.put("url", url);

        ObjectNode notification = objectMapper.createObjectNode();
        notification.put("title", title);
        notification.put("body", body);
        notification.put("tag", tag);
        notification.put("icon", "/assets/BRP_logo.png");
        notification.set("data", data);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("notification", notification);

        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        try {
            Notification pushNotification = new Notification(
                subscription.getEndpoint(),
                Utils.loadPublicKey(subscription.getP256dh()),
                Base64.getUrlDecoder().decode(subscription.getAuth()),
                json.getBytes()
            );

            LOGGER.debug("Subscription endpoint: " + subscription.getEndpoint());
            LOGGER.debug("Payload: " + json);

            pushService.send(pushNotification, Encoding.AES128GCM);
        } catch (Exception e) {
            LOGGER.error("Failed to send push notification", e);
            throw new RuntimeException(e);
        }
    }
}
