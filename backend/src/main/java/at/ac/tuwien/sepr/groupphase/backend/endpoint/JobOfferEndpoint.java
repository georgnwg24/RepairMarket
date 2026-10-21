package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferAcceptDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferDetailWithUsernameDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferSummaryDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page.PageDto;
import at.ac.tuwien.sepr.groupphase.backend.service.JobOfferService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.lang.invoke.MethodHandles;
import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping(path = JobOfferEndpoint.BASE_PATH)
public class JobOfferEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final String BASE_PATH = "/api/v1/job-offers";
    private final JobOfferService jobOfferService;

    @Autowired
    public JobOfferEndpoint(JobOfferService jobOfferService) {
        this.jobOfferService = jobOfferService;
    }

    @PermitAll
    @PostMapping(path = "/{jobRequestId}/offers", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> createJobOffer(
        @PathVariable Long jobRequestId,
        @RequestPart("jobOffer") @Valid JobOfferCreateDto jobOffer,
        BindingResult bindingResult) throws AccessDeniedException {

        LOGGER.info("POST " + BASE_PATH + "/" + jobRequestId + "/offers");
        LOGGER.debug("Received DTO: {}", jobOffer);

        if (bindingResult.hasErrors()) {
            // return 400 Bad Request if validation fails
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(bindingResult.getAllErrors());
        }

        JobOfferDetailDto created = jobOfferService.createJobOffer(jobRequestId, jobOffer);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PermitAll
    @GetMapping("/worker")
    public ResponseEntity<PageDto<JobOfferSummaryDto>> getOffersFromWorker(
        @RequestParam(defaultValue = "0") int offset,
        @RequestParam(defaultValue = "10") int limit
    ) {
        LOGGER.info("GET " + BASE_PATH + "/worker");

        try {
            return ResponseEntity.ok(jobOfferService.getOffersFromWorker(offset, limit));
        } catch (Exception e) {
            LOGGER.error("Error fetching job offers from worker: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PermitAll
    @GetMapping("/all")
    public ResponseEntity<List<JobOfferSummaryDto>> getAllOffers() {
        LOGGER.info("GET " + BASE_PATH + "/all");
        try {
            return ResponseEntity.ok(jobOfferService.getOffers());
        } catch (Exception e) {
            LOGGER.error("Error fetching job offers from worker: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }


    @PermitAll
    @PostMapping("/{offerId}/withdraw")
    public ResponseEntity<Void> withdrawOffer(@PathVariable Long offerId) {
        LOGGER.info("POST " + BASE_PATH + "/" + offerId + "/withdraw");

        jobOfferService.withdrawOffer(offerId);
        return ResponseEntity.ok().build();
    }

    @PermitAll
    @DeleteMapping("/{offerId}")
    public ResponseEntity<Void> deleteOffer(@PathVariable Long offerId) {
        LOGGER.info("DELETE " + BASE_PATH + "/" + offerId);

        jobOfferService.deleteOffer(offerId);
        return ResponseEntity.noContent().build();
    }

    @PermitAll
    @PostMapping("/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> acceptOffer(
        @RequestBody JobOfferAcceptDto dto) {
        LOGGER.info("POST " + BASE_PATH + "/accept");

        jobOfferService.acceptOffer(dto.getOfferId(), dto.getCreatedAt());
        return ResponseEntity.ok().build();
    }

    @PermitAll
    @GetMapping("/customer")
    public ResponseEntity<List<JobOfferDetailWithUsernameDto>> getOffersForCustomer() {
        LOGGER.info("GET " + BASE_PATH + "/customer");

        try {
            return ResponseEntity.ok(jobOfferService.getOffersForCustomer());
        } catch (Exception e) {
            LOGGER.error("Error fetching job offers for customer: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PermitAll
    @GetMapping("/{offerId}")
    public ResponseEntity<JobOfferCreateDto> getOfferById(@PathVariable Long offerId) {
        LOGGER.info("GET " + BASE_PATH + "/" + offerId);

        try {
            return ResponseEntity.ok(jobOfferService.getOfferById(offerId));
        } catch (Exception e) {
            LOGGER.error("Error fetching job offer {}: {}", offerId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found", e);
        }
    }

    @PermitAll
    @PutMapping(path = "/{offerId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> updateOffer(
        @PathVariable Long offerId,
        @RequestPart("jobOffer") @Valid JobOfferCreateDto updateDto,
        BindingResult bindingResult) throws AccessDeniedException {

        LOGGER.info("PUT " + BASE_PATH + "/" + offerId);
        LOGGER.debug("Received DTO: {}", updateDto);

        if (bindingResult.hasErrors()) {
            // return 400 Bad Request if validation fails
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(bindingResult.getAllErrors());
        }

        jobOfferService.updateOffer(offerId, updateDto);
        return ResponseEntity.ok().build();
    }
}
