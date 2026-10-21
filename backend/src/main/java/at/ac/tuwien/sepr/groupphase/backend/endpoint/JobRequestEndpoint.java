package at.ac.tuwien.sepr.groupphase.backend.endpoint;


import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestListWithMinPriceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestSearchDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestUpdateRestDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestWithCustomerDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page.PageDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.service.JobRequestService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;
import java.util.List;


@RestController
@RequestMapping(path = JobRequestEndpoint.BASE_PATH)
public class JobRequestEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final String BASE_PATH = "/api/v1/job-requests";
    private final JobRequestService jobRequestService;

    @Autowired
    public JobRequestEndpoint(JobRequestService jobRequestService) {
        this.jobRequestService = jobRequestService;
    }

    /**
     * Admin-scoped: list all job requests.
     */
    @PermitAll
    @GetMapping(path = "/all")
    public List<JobRequestListDto> listAllJobRequests() {
        return jobRequestService.findAll();
    }

    /**
     * Worker-scoped: list all open job requests if no open offer by this worker already exists.
     */
    @PermitAll
    @GetMapping
    public List<JobRequestListWithMinPriceDto> listAllOpenJobRequests() {
        return jobRequestService.findAllOpen();
    }

    @PermitAll
    @GetMapping(path = "/user")
    public List<JobRequestListDto> listJobRequests() {
        LOGGER.info("GET " + BASE_PATH);
        LOGGER.debug("request parameters: ");
        return jobRequestService.listJobRequests();
    }

    @PermitAll
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> create(
        @Valid @RequestBody JobRequestCreateDto toCreate,
        BindingResult bindingResult) {
        LOGGER.info("POST " + BASE_PATH);
        LOGGER.debug("Received Json: {}", toCreate);
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(bindingResult.getAllErrors());
        }
        LOGGER.debug("Body of request:\n{}", toCreate);
        JobRequestDetailDto created = jobRequestService.create(toCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);

    }

    @PermitAll
    @PutMapping(path = "/{id}/edit")
    public ResponseEntity<?> update(
        @PathVariable("id") Long id,
        @Valid @RequestBody JobRequestUpdateRestDto toUpdate,
        BindingResult bindingResult) {
        LOGGER.info("PUT " + BASE_PATH + "/{}, {}", id, toUpdate);
        LOGGER.debug("Received JSON: {}", toUpdate);
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(bindingResult.getAllErrors());
        }
        LOGGER.info("PUT " + BASE_PATH);
        LOGGER.debug("Body of request:\n{}", toUpdate);
        return ResponseEntity.ok(jobRequestService.update(toUpdate.updateWithId(id)));

    }

    @PermitAll
    @GetMapping("/{id}")
    public JobRequestDetailDto getJobRequest(@PathVariable("id") Long id) throws NotFoundException {
        LOGGER.info("GET " + BASE_PATH + "/{}", id);
        return jobRequestService.getById(id);
    }

    @PermitAll
    @GetMapping("/{id}/full")
    public JobRequestWithCustomerDto getJobRequestWithCustomer(@PathVariable("id") Long id) throws NotFoundException {
        LOGGER.info("GET " + BASE_PATH + "/{}/full", id);
        return jobRequestService.getByIdWithCustomer(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PermitAll
    public ResponseEntity<?> delete(@PathVariable("id") Long id) throws NotFoundException {
        jobRequestService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PermitAll
    @PutMapping("/{id}/done")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRequestDone(@PathVariable("id") Long id) {
        LOGGER.info("PUT " + BASE_PATH + "/{}/done", id);
        jobRequestService.markRequestDone(id);
    }

    @PermitAll
    @GetMapping("/user/search")
    public PageDto<JobRequestListDto> searchCustomerJobRequests(
        JobRequestSearchDto searchDto,
        @RequestParam(defaultValue = "0") int offset,
        @RequestParam(defaultValue = "10") int limit) {
        LOGGER.info("GET " + BASE_PATH + "/user/search");
        LOGGER.debug("Customer search parameters: {}", searchDto);
        return jobRequestService.searchJobRequestsCustomer(searchDto, offset, limit);
    }

    @PermitAll
    @GetMapping("/worker/search")
    public PageDto<JobRequestListWithMinPriceDto> searchWorkerJobRequests(
        JobRequestSearchDto searchDto,
        @RequestParam(defaultValue = "0") int offset,
        @RequestParam(defaultValue = "10") int limit) {
        LOGGER.info("GET " + BASE_PATH + "/worker/search");
        LOGGER.debug("Worker search parameters: {}", searchDto);
        return jobRequestService.searchJobRequestsWorker(searchDto, offset, limit);
    }

    @GetMapping("/admin/search")
    @PreAuthorize("hasRole('ADMIN')")
    public PageDto<JobRequestListDto> searchAdminJobRequests(
        JobRequestSearchDto searchDto,
        @RequestParam(defaultValue = "0") int offset,
        @RequestParam(defaultValue = "10") int limit) {
        LOGGER.info("GET " + BASE_PATH + "/admin/search");
        LOGGER.debug("Admin search parameters: {}", searchDto);
        return jobRequestService.searchJobRequestsAdmin(searchDto, offset, limit);
    }
}
