package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.pushsubscriptions.PushSubscriptionDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobOffer;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.entity.PushSubscription;
import at.ac.tuwien.sepr.groupphase.backend.entity.SearchAlert;
import at.ac.tuwien.sepr.groupphase.backend.repository.PushSubscriptionRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SearchAlertRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.PushNotificationService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.service.WebPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

@Service
public class PushNotificationServiceImpl implements PushNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final UserService userService;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final WebPushService webPushService;
    private final SearchAlertRepository searchAlertRepository;

    public PushNotificationServiceImpl(UserService userService, PushSubscriptionRepository pushSubscriptionRepository, WebPushService webPushService, SearchAlertRepository searchAlertRepository) {
        this.userService = userService;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.webPushService = webPushService;
        this.searchAlertRepository = searchAlertRepository;
    }

    @Override
    public void saveSubscription(PushSubscriptionDto dto) {
        LOGGER.trace("saveSubscription() with parameters: {}", dto);
        ApplicationUser user = userService.getCurrentUser();

        Optional<PushSubscription> existing = pushSubscriptionRepository.findByEndpoint(dto.getEndpoint());

        PushSubscription pushSubscription = existing.orElseGet(PushSubscription::new);
        pushSubscription.setAuth(dto.getAuth());
        pushSubscription.setEndpoint(dto.getEndpoint());
        pushSubscription.setP256dh(dto.getP256dh());
        pushSubscription.setUser(user);

        pushSubscriptionRepository.save(pushSubscription);
    }

    @Override
    public void notifyCustomerOfJobOffer(ApplicationUser customer, JobRequest jobRequest, JobOffer jobOffer) {
        LOGGER.trace("notifyCustomerOfJobOffer() with parameters: {}, {}, {}", customer, jobRequest, jobOffer);
        String title = "New job offer";
        String body = String.format("Your job request %s has a new offer of %.2f €", jobRequest.getTitle(), jobOffer.getPrice());
        String url = "/customer";
        String tag = "offer-" + jobOffer.getId();

        sendToAllSubscriptions(customer, title, body, url, tag);
    }

    @Override
    public void notifyWorkerOfAccept(ApplicationUser worker, JobRequest jobRequest) {
        LOGGER.trace("notifyWorkerOfAccept() with parameters: {}, {}", worker, jobRequest);
        String title = "Job offer accepted";
        String body = String.format("Your offer for %s was accepted", jobRequest.getTitle());
        String url = "/worker/offers";
        String tag = "request-" + jobRequest.getId();

        sendToAllSubscriptions(worker, title, body, url, tag);
    }

    @Override
    @Transactional
    public void notifyWorkersOfJobRequest(JobRequest jobRequest) {
        LOGGER.trace("notifyWorkersOfJobRequest() with {}", jobRequest);

        List<SearchAlert> matchingAlerts = searchAlertRepository.findMatchingAlertsForJobRequest(jobRequest.getId());

        if (matchingAlerts == null) {
            return;
        }

        for (SearchAlert alert : matchingAlerts) {
            alert.setCount(alert.getCount() + 1);
            if (!alert.isActive()) {
                continue;
            }
            ApplicationUser worker = alert.getWorker();
            String title = "New job request";
            String body = String.format("A new job '%s' matches your saved search.", jobRequest.getTitle());
            String url = "/worker/saved-searches";
            String tag = "saved-" + alert.getId();

            sendToAllSubscriptions(worker, title, body, url, tag);
        }
    }

    @Override
    public void notifyWorkerOfLicenseApproved(ApplicationUser worker) {
        LOGGER.trace("notifyWorkerOfLicenseApproved() with parameter: {}", worker);
        String title = "License approved";
        String body = "Your license was approved, you can now search for jobs!";
        String url = "/worker/requests";
        String tag = "license-approved";

        sendToAllSubscriptions(worker, title, body, url, tag);
    }

    @Override
    public void notifyWorkerOfLicenseRejected(ApplicationUser worker) {
        LOGGER.trace("notifyWorkerOfLicenseRejected() with parameter: {}", worker);
        String title = "License rejected";
        String body = "Your license was rejected, please upload a new one.";
        String url = "/worker/edit";
        String tag = "license-rejected";

        sendToAllSubscriptions(worker, title, body, url, tag);
    }

    @Override
    public void notifyAdminsOfLicense(Long licenseId, boolean isUpdate) {
        String title = isUpdate ? "License updated" : "New license uploaded";
        String body = isUpdate
            ? "A license has been updated and needs to be reviewed."
            : "A new license was uploaded and requires approval.";
        String url = "/admin/licenses";
        String tag = "license-" + licenseId;

        sendToAllAdmins(title, body, url, tag);
    }


    private void sendToAllSubscriptions(ApplicationUser user, String title, String body, String url, String tag) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findByUser(user);

        for (PushSubscription sub : subscriptions) {
            try {
                webPushService.sendNotification(sub, title, body, url, tag);
            } catch (Exception e) {
                LOGGER.error("Error while sending push notification: ", e);
            }
        }
    }

    private void sendToAllAdmins(String title, String body, String url, String tag) {
        List<PushSubscription> adminSubscriptions = pushSubscriptionRepository.findAllByAdminRole();

        if (adminSubscriptions != null) {
            for (PushSubscription subscription : adminSubscriptions) {
                try {
                    webPushService.sendNotification(subscription, title, body, url, tag);
                } catch (Exception e) {
                    LOGGER.error("Error while sending push notification: ", e);
                }
            }
        }
    }
}
