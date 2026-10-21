package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestListWithMinPriceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestSearchDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestWithCustomerDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page.PageDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;

import java.util.List;


public interface JobRequestService {

    /**
     * Updates the JobRequest with the ID given in {@code jobRequest}
     * with the data given in {@code jobRequest}
     * in the repository.
     *
     * @param jobRequest contains which JobRequest should be updated with what data
     * @return the updated JobRequest as an UpdateDto
     */
    JobRequestUpdateDto update(JobRequestUpdateDto jobRequest);

    /**
     * Fetches the JobRequest with the given ID.
     *
     * @param id of the wanted JobRequest
     * @return a JobRequestDetailDto containing the relevant information
     * @throws NotFoundException if the JobRequest does not exist
     */
    JobRequestDetailDto getById(long id) throws NotFoundException;

    /**
     * Fetches the JobRequest with the given ID with the customer ID and username.
     *
     * @param id of the wanted JobRequest
     * @return a JobRequestWithCustomerDto containing the relevant information
     * @throws NotFoundException if the JobRequest does not exist
     */
    JobRequestWithCustomerDto getByIdWithCustomer(long id) throws NotFoundException;

    /**
     * Lists all jobs stored in the system, that fit the parameters.
     *
     * @return list of all stored jobs
     */
    List<JobRequestListDto> listJobRequests();

    /**
     * Delete a JobRequest from persistent storage by ID, unless it was of status DONE, then set the status to HIDDEN.
     *
     * @param id the ID of the JobRequest to be deleted.
     * @throws NotFoundException if the JobRequest with given ID does not exist in the persistent data store.
     */
    void deleteById(long id) throws NotFoundException;


    JobRequestDetailDto create(JobRequestCreateDto toCreate);

    /**
     * Admin-scoped: list all job requests.
     */
    List<JobRequestListDto> findAll();

    /**
     * Worker-scoped: return every open job request in the system (which has no
     * open offer from this worker) with the lowest current offer.
     */
    List<JobRequestListWithMinPriceDto> findAllOpen();

    /**
     * Sets the status of the job request with the given ID to 'DONE'.
     *
     * @param id of the JobRequest
     * @throws NotFoundException if the JobRequest does not exist
     */
    void markRequestDone(Long id) throws NotFoundException;

    /**
     * Fetches the list of job requests adhering to the search parameters.
     *
     * @param searchDto containing the search parameters
     * @return a list of JobRequestListDto containing the relevant job requests
     */
    PageDto<JobRequestListDto> searchJobRequestsCustomer(JobRequestSearchDto searchDto, int offset, int limit);

    /**
     * Fetches the list of job requests adhering to the search parameters and worker open logic (see findAllOpen).
     *
     * @param searchDto containing the search parameters
     * @return a list of JobRequestListWithMinPriceDto containing the relevant job requests
     */
    PageDto<JobRequestListWithMinPriceDto> searchJobRequestsWorker(JobRequestSearchDto searchDto, int offset, int limit);

    PageDto<JobRequestListDto> searchJobRequestsAdmin(JobRequestSearchDto searchDto, int offset, int limit);
}
