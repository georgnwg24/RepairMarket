package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.pushsubscriptions.PushSubscriptionDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobOffer;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;

public interface PushNotificationService {

    /**
     * Saves an incoming push notification subscription. Acts as an update if the device
     * already registered a subscription (if the endpoint already exists in the db)
     */
    void saveSubscription(PushSubscriptionDto dto);

    /**
     * Sends the customer a push notification with the job request title and job offer amount.
     */
    void notifyCustomerOfJobOffer(ApplicationUser customer, JobRequest jobRequest, JobOffer jobOffer);

    /**
     * Sends the worker a push notification with the job request title.
     */
    void notifyWorkerOfAccept(ApplicationUser worker, JobRequest jobRequest);

    /**
     * Sends all workers with matching Search Alerts a push notification about the new job request.
     */
    void notifyWorkersOfJobRequest(JobRequest jobRequest);

    /**
     * Sends the worker a push notification regarding the new license status.
     */
    void notifyWorkerOfLicenseApproved(ApplicationUser worker);

    /**
     * Sends the worker a push notification regarding the new license status.
     */
    void notifyWorkerOfLicenseRejected(ApplicationUser worker);

    /**
     * Sends all admins a push notification regarding the license to be approved.
     */
    void notifyAdminsOfLicense(Long licenseId, boolean isUpdate);
}
