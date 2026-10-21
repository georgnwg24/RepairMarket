package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.pushsubscriptions.PushSubscriptionDto;
import at.ac.tuwien.sepr.groupphase.backend.service.PushNotificationService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;

@RestController
@RequestMapping(path = PushSubscriptionEndpoint.BASE_PATH)
public class PushSubscriptionEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final String BASE_PATH = "/api/v1/notifications";
    private final PushNotificationService pushNotificationService;

    @Autowired
    public PushSubscriptionEndpoint(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    @PermitAll
    @PostMapping
    public ResponseEntity<Void> saveSubscription(@Valid @RequestBody PushSubscriptionDto dto) {
        LOGGER.info("POST " + BASE_PATH);
        LOGGER.debug("Received Json: {}", dto);
        pushNotificationService.saveSubscription(dto);
        return ResponseEntity.ok().build();
    }
}
