package at.ac.tuwien.sepr.groupphase.backend.unittests.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferDetailWithUsernameDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferSummaryDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.JobOfferMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobOffer;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.exception.OfferAlreadyExistsException;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobOfferRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.PushNotificationService;
import at.ac.tuwien.sepr.groupphase.backend.service.RatingService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.JobOfferServiceImpl;
import at.ac.tuwien.sepr.groupphase.backend.type.JobOfferStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.JobStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyFloat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobOfferServiceTest {

    @Mock
    private JobOfferRepository jobOfferRepository;
    @Mock
    private JobRequestRepository jobRequestRepository;
    @Mock
    private UserService userService;
    @Mock
    private JobOfferMapper jobOfferMapper;
    @Mock
    private PushNotificationService pushNotificationService;
    @Mock
    private Validator validator;
    @Mock
    private RatingService ratingService;

    @InjectMocks
    private JobOfferServiceImpl jobOfferService;

    private ApplicationUser worker;
    private JobOfferCreateDto createDto;
    private JobRequest jobRequest;
    private JobOffer jobOffer;
    private ApplicationUser customer;

    @BeforeEach
    void setUp() {
        worker = ApplicationUser.builder()
            .id(1L)
            .role(Role.WORKER)
            .build();

        customer = ApplicationUser.builder()
            .id(2L)
            .role(Role.CUSTOMER)
            .build();

        jobRequest = JobRequest.builder()
            .id(1L)
            .customer(customer)
            .build();

        createDto = JobOfferCreateDto.builder()
            .price(100.0f)
            .comment("Test offer")
            .build();

        jobOffer = JobOffer.builder()
            .id(1L)
            .price(100.0f)
            .comment("Test offer")
            .jobRequest(jobRequest)
            .worker(worker)
            .status(JobOfferStatus.PENDING)
            .build();
    }

    @Test
    void createJobOffer_validData_shouldCreateOffer() throws AccessDeniedException {

        when(userService.getCurrentUser()).thenReturn(worker);
        when(jobOfferRepository.existsByJobRequestIdAndWorkerIdAndStatus(1L, 1L, JobOfferStatus.PENDING)).thenReturn(false);
        when(validator.validate(createDto)).thenReturn(Set.of());
        when(jobRequestRepository.findById(1L)).thenReturn(Optional.of(jobRequest));
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(jobOffer);
        when(jobOfferMapper.jobOfferToDetailDto(any(JobOffer.class))).thenReturn(new JobOfferDetailDto());

        var result = jobOfferService.createJobOffer(1L, createDto);

        assertNotNull(result);
        verify(jobOfferRepository).save(any(JobOffer.class));
        verify(pushNotificationService).notifyCustomerOfJobOffer(any(), any(), any());
    }

    @Test
    void createJobOffer_userIsNotWorker_shouldThrowAccessDeniedException() {
        worker.setRole(Role.CUSTOMER);
        when(userService.getCurrentUser()).thenReturn(worker);

        assertThrows(AccessDeniedException.class, () -> jobOfferService.createJobOffer(1L, createDto));
    }

    @Test
    void createJobOffer_alreadyExists_shouldThrowOfferAlreadyExistsException() {
        when(userService.getCurrentUser()).thenReturn(worker);
        when(jobOfferRepository.existsByJobRequestIdAndWorkerIdAndStatus(1L, 1L, JobOfferStatus.PENDING)).thenReturn(true);

        assertThrows(OfferAlreadyExistsException.class, () -> jobOfferService.createJobOffer(1L, createDto));
    }

    @Test
    void createJobOffer_invalidDto_shouldThrowConstraintViolationException() {
        when(userService.getCurrentUser()).thenReturn(worker);
        when(jobOfferRepository.existsByJobRequestIdAndWorkerIdAndStatus(1L, 1L, JobOfferStatus.PENDING)).thenReturn(false);

        Set<ConstraintViolation<JobOfferCreateDto>> violations = Set.of(mock(ConstraintViolation.class));
        when(validator.validate(createDto)).thenReturn(violations);

        assertThrows(ConstraintViolationException.class, () -> jobOfferService.createJobOffer(1L, createDto));
    }

    @Test
    void createJobOffer_jobRequestNotFound_shouldThrowEntityNotFoundException() {
        when(userService.getCurrentUser()).thenReturn(worker);
        when(jobOfferRepository.existsByJobRequestIdAndWorkerIdAndStatus(1L, 1L, JobOfferStatus.PENDING)).thenReturn(false);
        when(validator.validate(createDto)).thenReturn(Set.of());
        when(jobRequestRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> jobOfferService.createJobOffer(1L, createDto));
    }

    @Test
    void updateOffer_validData_shouldUpdateOffer() throws AccessDeniedException {
        when(userService.getCurrentUser()).thenReturn(worker);
        when(jobOfferRepository.findByIdWithWorkerAndRequest(1L)).thenReturn(jobOffer);
        when(validator.validate(createDto)).thenReturn(Set.of());
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(jobOffer);

        jobOfferService.updateOffer(1L, createDto);

        verify(jobOfferRepository).save(jobOffer);
        assertEquals(createDto.getPrice(), jobOffer.getPrice());
    }

    @Test
    void updateOffer_validData_priceChanged_shouldNotifyCustomer() throws AccessDeniedException {
        JobOfferCreateDto updatedDto = JobOfferCreateDto.builder()
            .price(150.0f)
            .comment("Updated offer")
            .build();
        when(userService.getCurrentUser()).thenReturn(worker);
        when(jobOfferRepository.findByIdWithWorkerAndRequest(1L)).thenReturn(jobOffer);
        when(validator.validate(updatedDto)).thenReturn(Set.of());
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(jobOffer);

        jobOfferService.updateOffer(1L, updatedDto);

        verify(jobOfferRepository).save(jobOffer);
        assertEquals(updatedDto.getPrice(), jobOffer.getPrice());
        verify(pushNotificationService).notifyCustomerOfJobOffer(any(ApplicationUser.class), any(JobRequest.class), any(JobOffer.class));
    }

    @Test
    void updateOffer_notFound_shouldThrowEntityNotFoundException() {
        when(jobOfferRepository.findByIdWithWorkerAndRequest(1L)).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> jobOfferService.updateOffer(1L, createDto));
    }

    @Test
    void updateOffer_wrongRole_shouldThrowAccessDeniedException() {
        worker.setRole(Role.CUSTOMER);
        when(userService.getCurrentUser()).thenReturn(worker);
        when(jobOfferRepository.findByIdWithWorkerAndRequest(1L)).thenReturn(jobOffer);

        assertThrows(AccessDeniedException.class, () -> jobOfferService.updateOffer(1L, createDto));
    }

    @Test
    void updateOffer_differentWorker_shouldThrowAccessDeniedException() {
        ApplicationUser anotherUser = ApplicationUser.builder().id(2L).role(Role.WORKER).build();
        when(userService.getCurrentUser()).thenReturn(anotherUser);
        when(jobOfferRepository.findByIdWithWorkerAndRequest(1L)).thenReturn(jobOffer);

        assertThrows(AccessDeniedException.class, () -> jobOfferService.updateOffer(1L, createDto));
    }

    @Test
    void updateOffer_notPending_shouldThrowIllegalStateException() {
        jobOffer.setStatus(JobOfferStatus.DONE);
        when(userService.getCurrentUser()).thenReturn(worker);
        when(jobOfferRepository.findByIdWithWorkerAndRequest(1L)).thenReturn(jobOffer);

        assertThrows(IllegalStateException.class, () -> jobOfferService.updateOffer(1L, createDto));
    }

    @Test
    void updateOffer_invalidDto_shouldThrowConstraintViolationException() {
        when(userService.getCurrentUser()).thenReturn(worker);
        when(jobOfferRepository.findByIdWithWorkerAndRequest(1L)).thenReturn(jobOffer);

        Set<ConstraintViolation<JobOfferCreateDto>> violations = Set.of(mock(ConstraintViolation.class));
        when(validator.validate(createDto)).thenReturn(violations);

        assertThrows(ConstraintViolationException.class, () -> jobOfferService.updateOffer(1L, createDto));
    }

/*
    @Test
    void getOffersFromWorker_validWorker_shouldReturnFilteredOffers() throws AccessDeniedException {
        worker.setRole(Role.WORKER);
        JobOffer anotherOffer = JobOffer.builder()
            .id(2L)
            .status(JobOfferStatus.PENDING)
            .jobRequest(jobRequest)
            .worker(worker)
            .build();
        jobRequest.setReceivedJobOffers(List.of(jobOffer, anotherOffer));
        jobOffer.setStatus(JobOfferStatus.PENDING);

        when(userService.getCurrentUser()).thenReturn(worker);
        when(jobOfferRepository.findAllByWorkerIdWithJobRequestAndOffers(worker.getId()))
            .thenReturn(List.of(jobOffer));
        when(jobOfferMapper.jobOfferToSummaryDto(any(JobOffer.class))).thenReturn(new JobOfferSummaryDto());

        var result = jobOfferService.getOffersFromWorker();

        assertEquals(1, result.size());
    }

 */
/*
    @Test
    void getOffersFromWorker_notWorker_shouldThrowAccessDeniedException() {
        worker.setRole(Role.CUSTOMER);
        when(userService.getCurrentUser()).thenReturn(worker);

        assertThrows(AccessDeniedException.class, () -> jobOfferService.getOffersFromWorker());
    }

 */

    @Test
    void getOffersForCustomer_validCustomer_shouldReturnFilteredOffers() throws AccessDeniedException {
        JobRequest acceptedRequest = JobRequest.builder().status(JobStatus.ACCEPTED).customer(customer).build();
        jobOffer.setJobRequest(acceptedRequest);
        jobOffer.setStatus(JobOfferStatus.ACCEPTED);

        when(userService.getCurrentUser()).thenReturn(customer);
        when(jobOfferRepository.findAllForCustomerWithWorker(customer.getId()))
            .thenReturn(List.of(jobOffer));

        when(ratingService.getRatingStats(any(Long.class)))
            .thenReturn(new at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.rating.RatingStatsDto(4.5f, 10));

        when(jobOfferMapper.jobOfferToDetailDtoWithWorker(any(JobOffer.class), anyFloat()))
            .thenReturn(new JobOfferDetailWithUsernameDto());

        var result = jobOfferService.getOffersForCustomer();

        assertEquals(1, result.size());
        verify(ratingService).getRatingStats(jobOffer.getWorker().getId());
        verify(jobOfferMapper).jobOfferToDetailDtoWithWorker(any(JobOffer.class), anyFloat());
    }

    @Test
    void getOffersForCustomer_invalidRole_shouldThrowAccessDeniedException() {
        worker.setRole(Role.WORKER);
        when(userService.getCurrentUser()).thenReturn(worker);

        assertThrows(AccessDeniedException.class, () -> jobOfferService.getOffersForCustomer());
    }

    @Test
    void getOfferById_validId_shouldReturnDto() {
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(jobOffer));
        when(jobOfferMapper.jobOfferToCreateDto(any(JobOffer.class))).thenReturn(createDto);

        var result = jobOfferService.getOfferById(1L);

        assertNotNull(result);
        assertEquals(createDto, result);
    }

    @Test
    void getOfferById_notFound_shouldThrowEntityNotFoundException() {
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> jobOfferService.getOfferById(1L));
    }

    @Test
    void withdrawOffer_validOffer_shouldUpdateStatus() {
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(jobOffer));
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(jobOffer);

        jobOfferService.withdrawOffer(1L);

        assertEquals(JobOfferStatus.WITHDRAWN, jobOffer.getStatus());
        verify(jobOfferRepository).save(any(JobOffer.class));
    }

    @Test
    void withdrawOffer_notFound_shouldThrowEntityNotFoundException() {
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> jobOfferService.withdrawOffer(1L));
    }



    @Test
    void acceptOffer_invalidStatus_shouldThrowIllegalStateException() {
        jobOffer.setStatus(JobOfferStatus.DONE);
        LocalDateTime now = LocalDateTime.now();
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(jobOffer));

        assertThrows(IllegalStateException.class, () -> jobOfferService.acceptOffer(1L, now));
    }

    @Test
    void acceptOffer_notFound_shouldThrowEntityNotFoundException() {
        LocalDateTime now = LocalDateTime.now();
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> jobOfferService.acceptOffer(1L, now));
    }

    @Test
    void deleteOffer_validStatus_shouldDeleteOffer() {
        jobOffer.setStatus(JobOfferStatus.REJECTED);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(jobOffer));
        doNothing().when(jobOfferRepository).delete(any(JobOffer.class));

        jobOfferService.deleteOffer(1L);

        verify(jobOfferRepository).delete(jobOffer);
    }

    @Test
    void deleteOffer_doneStatus_shouldHideOffer() {
        jobOffer.setStatus(JobOfferStatus.DONE);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(jobOffer));
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(jobOffer);

        jobOfferService.deleteOffer(1L);

        verify(jobOfferRepository).save(jobOffer);
        assertEquals(JobOfferStatus.HIDDEN, jobOffer.getStatus());
    }

    @Test
    void deleteOffer_invalidStatus_shouldThrowIllegalStateException() {
        jobOffer.setStatus(JobOfferStatus.PENDING);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(jobOffer));

        assertThrows(IllegalStateException.class, () -> jobOfferService.deleteOffer(1L));
    }

    @Test
    void deleteOffer_notFound_shouldThrowEntityNotFoundException() {
        when(jobOfferRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> jobOfferService.deleteOffer(999L));
    }

    @Test
    void updateOffer_priceNotChanged_shouldNotNotifyCustomer() throws AccessDeniedException {
        when(userService.getCurrentUser()).thenReturn(worker);
        when(jobOfferRepository.findByIdWithWorkerAndRequest(1L)).thenReturn(jobOffer);
        when(validator.validate(createDto)).thenReturn(Collections.emptySet());

        jobOfferService.updateOffer(1L, createDto);

        verify(jobOfferRepository).save(jobOffer);
        verify(pushNotificationService, never()).notifyCustomerOfJobOffer(any(), any(), any());
    }

    @Test
    void getOffersFromWorker_shouldNotReturnHiddenOffers() throws AccessDeniedException {
        JobOffer hiddenOffer = JobOffer.builder()
            .id(2L)
            .status(JobOfferStatus.HIDDEN)
            .jobRequest(jobRequest)
            .worker(worker)
            .build();
        jobOffer.setStatus(JobOfferStatus.DONE);

        jobRequest.setReceivedJobOffers(List.of(jobOffer, hiddenOffer));

        when(userService.getCurrentUser()).thenReturn(worker);
        when(jobOfferRepository.findAllByWorkerIdWithJobRequestAndOffers(worker.getId()))
            .thenReturn(List.of(jobOffer, hiddenOffer));
        when(jobOfferMapper.jobOfferToSummaryDto(jobOffer)).thenReturn(new JobOfferSummaryDto());

        var result = jobOfferService.getOffers();

        assertEquals(1, result.size());
        verify(jobOfferMapper).jobOfferToSummaryDto(jobOffer);
        verify(jobOfferMapper, never()).jobOfferToSummaryDto(hiddenOffer);
    }

    @Test
    void getOffersForCustomer_forPendingRequest_shouldReturnPendingOffers() throws AccessDeniedException {
        jobRequest.setStatus(JobStatus.PENDING);
        JobOffer pendingOffer = jobOffer; // This is already PENDING
        JobOffer withdrawnOffer = JobOffer.builder().id(2L).status(JobOfferStatus.WITHDRAWN).jobRequest(jobRequest).worker(worker).build();
        jobRequest.setReceivedJobOffers(List.of(pendingOffer, withdrawnOffer));

        when(userService.getCurrentUser()).thenReturn(customer);
        when(jobOfferRepository.findAllForCustomerWithWorker(customer.getId())).thenReturn(List.of(pendingOffer, withdrawnOffer));
        when(ratingService.getRatingStats(any(Long.class))).thenReturn(new at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.rating.RatingStatsDto(5.0f, 1));
        when(jobOfferMapper.jobOfferToDetailDtoWithWorker(any(JobOffer.class), anyFloat())).thenReturn(new JobOfferDetailWithUsernameDto());

        var result = jobOfferService.getOffersForCustomer();

        assertEquals(1, result.size());
        verify(jobOfferMapper).jobOfferToDetailDtoWithWorker(pendingOffer, 5.0f);
        verify(jobOfferMapper, never()).jobOfferToDetailDtoWithWorker(withdrawnOffer, 5.0f);
    }

    @Test
    void getOffersForCustomer_noOffers_shouldReturnEmptyList() throws AccessDeniedException {
        when(userService.getCurrentUser()).thenReturn(customer);
        when(jobOfferRepository.findAllForCustomerWithWorker(customer.getId())).thenReturn(Collections.emptyList());

        var result = jobOfferService.getOffersForCustomer();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteOffer_withdrawnStatus_shouldDeleteOffer() {
        jobOffer.setStatus(JobOfferStatus.WITHDRAWN);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(jobOffer));
        doNothing().when(jobOfferRepository).delete(any(JobOffer.class));

        jobOfferService.deleteOffer(1L);

        verify(jobOfferRepository).delete(jobOffer);
    }

    @Test
    void withdrawOffer_notPending_shouldThrowIllegalStateException() {
        jobOffer.setStatus(JobOfferStatus.ACCEPTED);
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(jobOffer));

        assertThrows(IllegalStateException.class, () -> jobOfferService.withdrawOffer(1L));
    }
}