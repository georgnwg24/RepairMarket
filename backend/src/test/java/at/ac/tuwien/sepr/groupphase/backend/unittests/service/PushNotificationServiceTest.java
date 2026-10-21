package at.ac.tuwien.sepr.groupphase.backend.unittests.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.pushsubscriptions.PushSubscriptionDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobOffer;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.entity.PushSubscription;
import at.ac.tuwien.sepr.groupphase.backend.entity.SearchAlert;
import at.ac.tuwien.sepr.groupphase.backend.repository.PushSubscriptionRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SearchAlertRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.service.WebPushService;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.PushNotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private PushSubscriptionRepository pushSubscriptionRepository;
    @Mock
    private WebPushService webPushService;
    @Mock
    private SearchAlertRepository searchAlertRepository;

    @InjectMocks
    private PushNotificationServiceImpl pushNotificationService;

    private ApplicationUser testUser;
    private JobRequest testJobRequest;

    @BeforeEach
    void setUp() {
        testUser = new ApplicationUser();
        testUser.setId(1L);
        testUser.setEmail("test@user.com");

        testJobRequest = new JobRequest();
        testJobRequest.setId(10L);
        testJobRequest.setTitle("Test Job Request");
    }

    @Test
    void saveSubscription_whenSubscriptionIsNew_createsAndSavesNewEntity() {
        PushSubscriptionDto dto = new PushSubscriptionDto("http://new.endpoint", "new_p256dh", "new_auth");
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(pushSubscriptionRepository.findByEndpoint(dto.getEndpoint())).thenReturn(Optional.empty());

        pushNotificationService.saveSubscription(dto);

        ArgumentCaptor<PushSubscription> captor = ArgumentCaptor.forClass(PushSubscription.class);
        verify(pushSubscriptionRepository).save(captor.capture());

        PushSubscription saved = captor.getValue();
        assertEquals(dto.getEndpoint(), saved.getEndpoint());
        assertEquals(dto.getP256dh(), saved.getP256dh());
        assertEquals(dto.getAuth(), saved.getAuth());
        assertEquals(testUser, saved.getUser());
    }

    @Test
    void saveSubscription_whenSubscriptionExists_updatesAndSavesExistingEntity() {
        PushSubscriptionDto dto = new PushSubscriptionDto("http://existing.endpoint", "updated_p256dh", "updated_auth");
        PushSubscription existingSubscription = new PushSubscription();
        existingSubscription.setEndpoint("http://existing.endpoint");
        existingSubscription.setUser(new ApplicationUser()); // old user

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(pushSubscriptionRepository.findByEndpoint(dto.getEndpoint())).thenReturn(Optional.of(existingSubscription));

        pushNotificationService.saveSubscription(dto);

        ArgumentCaptor<PushSubscription> captor = ArgumentCaptor.forClass(PushSubscription.class);
        verify(pushSubscriptionRepository).save(captor.capture());

        PushSubscription saved = captor.getValue();
        assertEquals(existingSubscription, saved);
        assertEquals(dto.getP256dh(), saved.getP256dh());
        assertEquals(dto.getAuth(), saved.getAuth());
        assertEquals(testUser, saved.getUser());
    }

    @Test
    void notifyCustomerOfJobOffer_sendsCorrectlyFormattedNotification() {
        // --- Arrange ---
        JobOffer jobOffer = new JobOffer();
        jobOffer.setId(20L);
        float price = 150.50f;
        jobOffer.setPrice(price);

        PushSubscription sub = new PushSubscription();
        when(pushSubscriptionRepository.findByUser(testUser)).thenReturn(List.of(sub));

        pushNotificationService.notifyCustomerOfJobOffer(testUser, testJobRequest, jobOffer);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(webPushService).sendNotification(any(PushSubscription.class), anyString(), bodyCaptor.capture(), anyString(), anyString());

        // dynamically format the expected price using the system's default locale.
        // this ensures the decimal separator (',' or '.') matches the application's output.
        String expectedPrice = String.format(Locale.getDefault(), "%.2f", price);
        String expectedBody = "Your job request Test Job Request has a new offer of " + expectedPrice + " €";

        assertEquals(expectedBody, bodyCaptor.getValue());
    }

    @Test
    void notifyWorkerOfAccept_sendsCorrectlyFormattedNotification() {
        PushSubscription sub = new PushSubscription();
        when(pushSubscriptionRepository.findByUser(testUser)).thenReturn(List.of(sub));

        pushNotificationService.notifyWorkerOfAccept(testUser, testJobRequest);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(webPushService).sendNotification(any(PushSubscription.class), anyString(), bodyCaptor.capture(), anyString(), anyString());

        String expectedBody = "Your offer for Test Job Request was accepted";
        assertEquals(expectedBody, bodyCaptor.getValue());
    }

    @Test
    void notifyWorkersOfJobRequest_whenMatchingActiveAlertsExist_sendsNotificationsAndIncrementsCount() {
        SearchAlert activeAlert = new SearchAlert();
        activeAlert.setWorker(testUser);
        activeAlert.setCount(5);
        activeAlert.setActive(true);

        when(searchAlertRepository.findMatchingAlertsForJobRequest(testJobRequest.getId())).thenReturn(List.of(activeAlert));
        when(pushSubscriptionRepository.findByUser(testUser)).thenReturn(List.of(new PushSubscription()));

        pushNotificationService.notifyWorkersOfJobRequest(testJobRequest);

        verify(webPushService, times(1)).sendNotification(any(), any(), any(), any(), any());
        assertEquals(6, activeAlert.getCount());
    }

    @Test
    void notifyWorkersOfJobRequest_whenMatchingInactiveAlertsExist_incrementsCountButSendsNoNotification() {
        SearchAlert inactiveAlert = new SearchAlert();
        inactiveAlert.setWorker(testUser);
        inactiveAlert.setCount(5);
        inactiveAlert.setActive(false); // alert is inactive

        when(searchAlertRepository.findMatchingAlertsForJobRequest(testJobRequest.getId())).thenReturn(List.of(inactiveAlert));

        pushNotificationService.notifyWorkersOfJobRequest(testJobRequest);

        verify(webPushService, never()).sendNotification(any(), any(), any(), any(), any());
        assertEquals(6, inactiveAlert.getCount());
    }

    @Test
    void notifyWorkersOfJobRequest_whenNoMatchingAlerts_doesNothing() {
        when(searchAlertRepository.findMatchingAlertsForJobRequest(testJobRequest.getId())).thenReturn(Collections.emptyList());

        pushNotificationService.notifyWorkersOfJobRequest(testJobRequest);

        verify(webPushService, never()).sendNotification(any(), any(), any(), any(), any());
    }

    @Test
    void sendToAllSubscriptions_whenOneFails_continuesToNext() {
        PushSubscription sub1 = new PushSubscription();
        PushSubscription sub2 = new PushSubscription();
        when(pushSubscriptionRepository.findByUser(testUser)).thenReturn(List.of(sub1, sub2));

        doThrow(new RuntimeException("Push failed!")).when(webPushService).sendNotification(sub1, "t", "b", "u", "t");

        pushNotificationService.notifyWorkerOfAccept(testUser, testJobRequest);

        verify(webPushService, times(1)).sendNotification(
            sub1, "Job offer accepted", "Your offer for Test Job Request was accepted", "/worker/offers", "request-10");
        verify(webPushService, times(1)).sendNotification(
            sub2, "Job offer accepted", "Your offer for Test Job Request was accepted", "/worker/offers", "request-10");
    }
}