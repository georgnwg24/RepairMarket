package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferDetailWithUsernameDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferSummaryDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page.PageDto;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;

public interface JobOfferService {
    /**
     * Create a new job offer on the given request by the logged in worker.
     *
     * @param jobRequestId the ID of the JobRequest to offer on
     * @param createDto    the DTO containing price and comment
     * @return the persisted JobOfferDetailDto
     */
    JobOfferDetailDto createJobOffer(Long jobRequestId, JobOfferCreateDto createDto) throws AccessDeniedException;

    /**
     * Lists all non-hidden offers sent from the logged in worker as page.
     *
     * @return list of all sent offers
     */
    PageDto<JobOfferSummaryDto> getOffersFromWorker(int offset, int limit) throws AccessDeniedException;


    /**
     * Lists all non-hidden offers sent from the logged in worker.
     *
     * @return list of all sent offers
     */
    List<JobOfferSummaryDto> getOffers() throws AccessDeniedException;

    /**
     * Delete a JobOffer from the persistent data store unless it was already DONE,
     * then only soft-delete it by setting its state to HIDDEN.
     *
     * @param id the ID of the JobOffer to be deleted.
     * @throws IllegalStateException if the JobOffer with given ID is not in either state: REJECTED, WITHDRAWN or DONE.
     */
    void deleteOffer(Long id);

    /**
     * Withdraw a JobOffer by setting its state to WITHDRAWN.
     *
     * @param id the ID of the JobOffer to be withdrawn.
     */
    void withdrawOffer(Long id);

    /**
     * Accept the job offer with the given ID and simultaneously reject all other job offers of its job request.
     * Also set the status of the corresponding job request to 'ACCEPTED'.
     *
     * @param id        the ID of the JobOffer to be accepted.
     * @param createdAt the timestamp of the offer which the user saw in the frontend to check possible updates
     */
    void acceptOffer(Long id, LocalDateTime createdAt);

    /**
     * Lists all offers sent for a job request of the logged in customer by the following logic:
     * For PENDING job requests, send all PENDING offers.
     * For ACCEPTED or DONE requests, send the single ACCEPTED offer (which could also be HIDDEN if the customer deleted it already).
     *
     * @return list of all received offers
     */
    List<JobOfferDetailWithUsernameDto> getOffersForCustomer() throws AccessDeniedException;

    /**
     * Return a single JobOffer by its ID.
     *
     * @param id the ID of the JobOffer to be returned.
     * @return the stored JobOfferCreateDto
     */
    JobOfferCreateDto getOfferById(Long id);

    /**
     * Update the job offer with the given ID.
     *
     * @param id        the ID of the JobOffer to update
     * @param updateDto the DTO containing price and comment
     */
    void updateOffer(Long id, JobOfferCreateDto updateDto) throws AccessDeniedException;

}
