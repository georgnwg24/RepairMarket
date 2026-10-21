package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferDetailWithUsernameDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferSummaryDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page.PageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.JobOfferMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobOffer;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.exception.OfferAlreadyExistsException;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobOfferRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.JobOfferService;
import at.ac.tuwien.sepr.groupphase.backend.service.PushNotificationService;
import at.ac.tuwien.sepr.groupphase.backend.service.RatingService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.type.JobOfferStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.JobStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JobOfferServiceImpl implements JobOfferService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final JobOfferRepository jobOfferRepository;
    private final JobRequestRepository jobRequestRepository;
    private final UserService userService;
    private final JobOfferMapper jobOfferMapper;
    private final Validator validator;
    private final PushNotificationService pushNotificationService;
    private final RatingService ratingService;

    @Autowired
    public JobOfferServiceImpl(JobOfferRepository jobOfferRepository,
                               JobRequestRepository jobRequestRepository,
                               UserService userService,
                               JobOfferMapper jobOfferMapper,
                               Validator validator,
                               PushNotificationService pushNotificationService,
                               RatingService ratingService) {
        this.jobOfferRepository = jobOfferRepository;
        this.jobRequestRepository = jobRequestRepository;
        this.userService = userService;
        this.jobOfferMapper = jobOfferMapper;
        this.validator = validator;
        this.pushNotificationService = pushNotificationService;
        this.ratingService = ratingService;
    }

    @Override
    public JobOfferDetailDto createJobOffer(Long jobRequestId, JobOfferCreateDto createDto) throws AccessDeniedException {
        LOGGER.trace("create() with parameters: {}", createDto);

        ApplicationUser worker = userService.getCurrentUser();

        if (!worker.getRole().equals(Role.WORKER)) {
            throw new AccessDeniedException("Only workers can make offer on job requests");
        }

        if (jobOfferRepository.existsByJobRequestIdAndWorkerIdAndStatus(jobRequestId, worker.getId(), JobOfferStatus.PENDING)) {
            throw new OfferAlreadyExistsException(
                "You have already submitted an offer for this request."
            );
        }

        Set<ConstraintViolation<JobOfferCreateDto>> violations = validator.validate(createDto);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        JobRequest request = jobRequestRepository.findById(jobRequestId)
            .orElseThrow(() -> new EntityNotFoundException(
                "JobRequest with id " + jobRequestId + " not found"));

        JobOffer offer = JobOffer.builder()
            .jobRequest(request)
            .worker(worker)
            .price(createDto.getPrice())
            .comment(createDto.getComment())
            .build();

        JobOffer saved = jobOfferRepository.save(offer);

        // send a push notification
        pushNotificationService.notifyCustomerOfJobOffer(request.getCustomer(), request, saved);

        return jobOfferMapper.jobOfferToDetailDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<JobOfferSummaryDto> getOffersFromWorker(int offset, int limit) throws AccessDeniedException {
        ApplicationUser worker = userService.getCurrentUser();

        if (!worker.getRole().equals(Role.WORKER)) {
            throw new AccessDeniedException("Only workers can see their sent job offers");
        }
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<JobOffer> offers = jobOfferRepository.findAllByWorkerIdAndStatusNotWithJobRequest(
            worker.getId(), JobOfferStatus.HIDDEN, pageable
        );
        return new PageDto<>(offers.stream()
            .map(offer -> {
                JobRequest jobRequest = offer.getJobRequest();
                jobRequest.getReceivedJobOffers().size();
                JobOfferSummaryDto dto = jobOfferMapper.jobOfferToSummaryDto(offer);
                dto.setLowestPrice(findLowestPrice(jobRequest));
                return dto;
            })
            .collect(Collectors.toList()),
            (int) offers.getTotalElements(), limit, offset);
    }

    public List<JobOfferSummaryDto> getOffers() throws AccessDeniedException {
        ApplicationUser worker = userService.getCurrentUser();

        if (!worker.getRole().equals(Role.WORKER)) {
            throw new AccessDeniedException("Only workers can see their sent job offers");
        }

        List<JobOffer> offers = jobOfferRepository.findAllByWorkerIdWithJobRequestAndOffers(worker.getId());

        return offers.stream()
            .filter(offer -> offer.getStatus() != JobOfferStatus.HIDDEN)
            .map(offer -> {
                JobOfferSummaryDto dto = jobOfferMapper.jobOfferToSummaryDto(offer);
                dto.setLowestPrice(findLowestPrice(offer.getJobRequest()));
                return dto;
            })
            .collect(Collectors.toList());
    }


    private Float findLowestPrice(JobRequest jobRequest) {
        return jobRequest.getReceivedJobOffers().stream()
            .filter(offer -> offer.getStatus() == JobOfferStatus.PENDING)
            .map(JobOffer::getPrice)
            .min(Float::compare)
            .orElse(0.0f);
    }

    @Override
    public void deleteOffer(Long id) {
        JobOffer offer = jobOfferRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(
                "JobOffer with id " + id + " not found"));

        if (offer.getStatus() != JobOfferStatus.REJECTED && offer.getStatus() != JobOfferStatus.WITHDRAWN && offer.getStatus() != JobOfferStatus.DONE) {
            throw new IllegalStateException("Only offers with state REJECTED, WITHDRAWN or DONE can be deleted");
        }

        if (offer.getStatus() == JobOfferStatus.DONE) {
            offer.setStatus(JobOfferStatus.HIDDEN);
            jobOfferRepository.save(offer);
        } else {
            jobOfferRepository.delete(offer);
        }
    }

    @Transactional
    @Override
    public void withdrawOffer(Long id) {
        JobOffer offer = jobOfferRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(
                "JobOffer with id " + id + " not found"));

        if (offer.getStatus() != JobOfferStatus.PENDING) {
            throw new IllegalStateException("The offer is not pending anymore!");
        }

        offer.setStatus(JobOfferStatus.WITHDRAWN);
        jobOfferRepository.save(offer);
    }

    @Transactional
    public void acceptOffer(Long id, LocalDateTime createdAt) {
        JobOffer offer = jobOfferRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(
                "JobOffer with id " + id + " not found"));

        if (offer.getStatus() != JobOfferStatus.PENDING) {
            throw new IllegalStateException("The offer is not pending anymore!");
        }

        /*if (!offer.getCreatedAt().equals(createdAt)) {
            throw new IllegalStateException("The job offer has been updated since it was last seen.");
        }*/

        offer.setStatus(JobOfferStatus.ACCEPTED);
        jobOfferRepository.save(offer);

        JobRequest jobRequest = offer.getJobRequest();
        for (JobOffer other : jobRequest.getReceivedJobOffers()) {
            if (!other.getId().equals(offer.getId())) {
                other.setStatus(JobOfferStatus.REJECTED);
                jobOfferRepository.save(other);
            }
        }

        jobRequest.setStatus(JobStatus.ACCEPTED);

        pushNotificationService.notifyWorkerOfAccept(offer.getWorker(), jobRequest);
    }

    @Override
    public List<JobOfferDetailWithUsernameDto> getOffersForCustomer() throws AccessDeniedException {
        ApplicationUser customer = userService.getCurrentUser();

        if (!customer.getRole().equals(Role.CUSTOMER)) {
            throw new AccessDeniedException("Only customers can see their received job offers");
        }

        List<JobOffer> allOffers = jobOfferRepository.findAllForCustomerWithWorker(customer.getId());

        return allOffers.stream()
            .filter(offer -> {
                JobStatus jobStatus = offer.getJobRequest().getStatus();
                JobOfferStatus offerStatus = offer.getStatus();

                if (jobStatus == JobStatus.PENDING) {
                    return offerStatus == JobOfferStatus.PENDING;
                }
                if (jobStatus == JobStatus.ACCEPTED || jobStatus == JobStatus.DONE) {
                    return offerStatus == JobOfferStatus.ACCEPTED || offerStatus == JobOfferStatus.DONE || offerStatus == JobOfferStatus.HIDDEN;
                }
                return false;
            })
            .map(offer -> {
                Float workerAverageRating = ratingService.getRatingStats(offer.getWorker().getId()).getAverage();
                return jobOfferMapper.jobOfferToDetailDtoWithWorker(offer, workerAverageRating);
            })
            .collect(Collectors.toList());
    }


    @Override
    public JobOfferCreateDto getOfferById(Long id) {
        LOGGER.debug("Fetching job offer with id {}", id);

        JobOffer offer = jobOfferRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("JobOffer with id " + id + " not found"));

        return jobOfferMapper.jobOfferToCreateDto(offer);
    }

    @Override
    @Transactional
    public void updateOffer(Long id, JobOfferCreateDto updateDto) throws AccessDeniedException {
        LOGGER.debug("Updating job offer {} with {}", id, updateDto);

        ApplicationUser worker = userService.getCurrentUser();
        JobOffer offer = jobOfferRepository.findByIdWithWorkerAndRequest(id);

        if (offer == null) {
            throw new EntityNotFoundException("JobOffer with id " + id + " not found");
        }

        if (!worker.getRole().equals(Role.WORKER)) {
            throw new AccessDeniedException("Only workers can make offer on job requests");
        }

        if (!offer.getWorker().getId().equals(worker.getId())) {
            throw new AccessDeniedException("You can only update your own offers");
        }

        if (offer.getStatus() != JobOfferStatus.PENDING) {
            throw new IllegalStateException("Only pending offers can be updated");
        }

        Set<ConstraintViolation<JobOfferCreateDto>> violations = validator.validate(updateDto);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        offer.setCreatedAt(LocalDateTime.now());

        boolean priceUpdated = offer.getPrice() != updateDto.getPrice();

        offer.setPrice(updateDto.getPrice());
        offer.setComment(updateDto.getComment());

        JobOffer saved = jobOfferRepository.save(offer);

        if (priceUpdated) {
            // send a push notification
            JobRequest request = saved.getJobRequest();
            pushNotificationService.notifyCustomerOfJobOffer(request.getCustomer(), request, saved);
        }
    }

}