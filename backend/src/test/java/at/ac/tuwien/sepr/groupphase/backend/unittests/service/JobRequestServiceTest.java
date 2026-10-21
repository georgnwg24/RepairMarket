package at.ac.tuwien.sepr.groupphase.backend.unittests.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestListWithMinPriceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestSearchDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestWithCustomerDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page.PageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.JobRequestMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobOffer;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.entity.Property;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobOfferRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PropertyRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReportRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.LicenseService;
import at.ac.tuwien.sepr.groupphase.backend.service.PushNotificationService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.JobRequestServiceImpl;
import at.ac.tuwien.sepr.groupphase.backend.type.Category;
import at.ac.tuwien.sepr.groupphase.backend.type.JobOfferStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.JobStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JobRequestServiceTest {

    @Mock
    private JobRequestRepository jobRequestRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private JobRequestMapper jobRequestMapper;
    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JobOfferRepository jobOfferRepository;
    @Mock
    private LicenseService licenseService;
    @Mock
    private PushNotificationService pushNotificationService;
    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private JobRequestServiceImpl jobRequestService;

    private static final Long USER_ID = 1L;
    private static final Long JOB_REQUEST_ID = 1L;
    private static final Long PROPERTY_ID = 1L;
    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(10);

    private ApplicationUser user;
    private ApplicationUser worker;
    private JobRequest jobRequest;
    private JobRequestCreateDto createDto;
    private JobRequestUpdateDto updateDto;
    private JobRequestSearchDto searchDto;

    @BeforeEach
    void setUp() {
        user = ApplicationUser.builder()
            .id(USER_ID)
            .username("testuser")
            .role(Role.CUSTOMER)
            .build();

        jobRequest = JobRequest.builder()
            .id(JOB_REQUEST_ID)
            .title("Test Job")
            .description("Test Description")
            .category(Category.PLUMBING)
            .status(JobStatus.PENDING)
            .deadline(FUTURE_DATE)
            .customer(user)
            .receivedJobOffers(Collections.emptyList())
            .build();

        createDto = JobRequestCreateDto.builder()
            .title("New Job")
            .description("New Description")
            .category(Category.ELECTRICAL)
            .status(JobStatus.PENDING)
            .deadline(FUTURE_DATE)
            .build();

        updateDto = JobRequestUpdateDto.builder()
            .id(JOB_REQUEST_ID)
            .title("Updated Job")
            .description("Updated Description")
            .category(Category.PAINTING)
            .status(JobStatus.ACCEPTED)
            .deadline(FUTURE_DATE.plusDays(5))
            .build();

        searchDto = JobRequestSearchDto.builder()
            .title("Test")
            .category("PLUMBING")
            .deadline(LocalDate.now())
            .build();

        worker = ApplicationUser.builder().id(2L).role(Role.WORKER).latitude(10f).longitude(10f).build();
    }

    @Test
    void create_withValidData_shouldReturnDetailDto() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(jobRequestRepository.save(any(JobRequest.class))).thenReturn(jobRequest);
        when(jobRequestMapper.jobRequestToDetailDto(any())).thenReturn(new JobRequestDetailDto());

        JobRequestDetailDto result = jobRequestService.create(createDto);

        assertNotNull(result);
        verify(jobRequestRepository).save(any(JobRequest.class));
    }

    @Test
    void create_withProperty_shouldSetProperty() {
        when(userService.getCurrentUser()).thenReturn(user);
        Property property = new Property();
        property.setId(PROPERTY_ID);

        createDto.setPropertyId(PROPERTY_ID);
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
        when(jobRequestRepository.save(any())).thenReturn(jobRequest);
        when(jobRequestMapper.jobRequestToDetailDto(any())).thenReturn(new JobRequestDetailDto());

        jobRequestService.create(createDto);

        verify(propertyRepository).findById(PROPERTY_ID);
    }

    @Test
    void create_withInvalidProperty_shouldThrowNotFoundException() {
        when(userService.getCurrentUser()).thenReturn(user);
        createDto.setPropertyId(PROPERTY_ID);

        assertThrows(NotFoundException.class, () -> jobRequestService.create(createDto));
    }

    @Test
    void update_whenValid_shouldReturnUpdateDto() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));
        when(jobRequestRepository.save(any())).thenReturn(jobRequest);
        when(jobRequestMapper.jobRequestToUpdateDto(any())).thenReturn(updateDto);

        JobRequestUpdateDto result = jobRequestService.update(updateDto);

        assertNotNull(result);
        assertEquals("Updated Job", result.getTitle());
    }

    @Test
    void update_whenNotOwner_shouldThrowAccessDenied() {
        when(userService.getCurrentUser()).thenReturn(user);
        ApplicationUser otherUser = ApplicationUser.builder().id(2L).build();
        jobRequest.setCustomer(otherUser);
        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));

        assertThrows(AccessDeniedException.class, () -> jobRequestService.update(updateDto));
    }

    @Test
    void getById_withExistingId_shouldReturnDetailDto() {
        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));
        when(jobRequestMapper.jobRequestToDetailDto(jobRequest)).thenReturn(new JobRequestDetailDto());

        JobRequestDetailDto result = jobRequestService.getById(JOB_REQUEST_ID);

        assertNotNull(result);
    }

    @Test
    void getById_withNonExistingId_shouldThrowNotFoundException() {
        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> jobRequestService.getById(JOB_REQUEST_ID));
    }

    @Test
    void listJobRequests_shouldReturnListDto() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(userRepository.findUserByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(jobRequestRepository.findAllByCustomerIdAndCustomerBannedFalse(USER_ID)).thenReturn(List.of(jobRequest));
        when(jobRequestMapper.jobRequestToListDto(anyList())).thenReturn(List.of(new JobRequestListDto()));

        List<JobRequestListDto> result = jobRequestService.listJobRequests();

        assertFalse(result.isEmpty());
    }

    @Test
    void deleteById_whenOwner_shouldDelete() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userService.getCurrentUser()).thenReturn(user);
        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));

        assertDoesNotThrow(() -> jobRequestService.deleteById(JOB_REQUEST_ID));

        verify(jobRequestRepository).delete(jobRequest);
    }

    @Test
    void findAllOpen_shouldFilterWorkerOffers() {
        ApplicationUser worker = ApplicationUser.builder().id(2L).role(Role.WORKER).build();

        JobOffer pendingOffer = new JobOffer();
        pendingOffer.setWorker(worker);
        pendingOffer.setStatus(JobOfferStatus.PENDING);

        jobRequest.setReceivedJobOffers(List.of(pendingOffer));

        when(userService.getCurrentUser()).thenReturn(worker);

        List<JobRequestListWithMinPriceDto> result = jobRequestService.findAllOpen();

        assertTrue(result.isEmpty(), "Should filter out requests with pending offers from the current worker");
    }

    @Test
    void markRequestDone_whenValid_shouldUpdateStatus() {
        when(userService.getCurrentUser()).thenReturn(user);
        JobOffer acceptedOffer = new JobOffer();
        acceptedOffer.setStatus(JobOfferStatus.ACCEPTED);

        jobRequest.setReceivedJobOffers(List.of(acceptedOffer));

        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));
        when(jobOfferRepository.findByJobRequestIdAndStatus(JOB_REQUEST_ID, JobOfferStatus.ACCEPTED))
            .thenReturn(acceptedOffer);

        assertDoesNotThrow(() -> jobRequestService.markRequestDone(JOB_REQUEST_ID));

        assertEquals(JobStatus.DONE, jobRequest.getStatus());
        assertEquals(JobOfferStatus.DONE, acceptedOffer.getStatus());
        verify(jobRequestRepository).save(jobRequest);
        verify(jobOfferRepository).save(acceptedOffer);
    }

    @Test
    void markRequestDone_whenNotOwner_shouldThrowAccessDenied() {
        when(userService.getCurrentUser()).thenReturn(user);
        ApplicationUser otherUser = ApplicationUser.builder().id(2L).build();
        jobRequest.setCustomer(otherUser);

        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));

        assertThrows(AccessDeniedException.class, () -> jobRequestService.markRequestDone(JOB_REQUEST_ID));
    }
    @Test
    void searchJobRequestsCustomer_shouldReturnFilteredResults() {
        when(userService.getCurrentUser()).thenReturn(user);

        Page<JobRequest> jobRequestPage = new PageImpl<>(List.of(jobRequest), PageRequest.of(0, 10), 1);
        when(jobRequestRepository.searchJobRequestsCustomer(
            eq(user.getId()), any(), any(), any(), any(), any(), any())
        ).thenReturn(jobRequestPage);

        when(jobRequestMapper.jobRequestToListDto(jobRequestPage.getContent()))
            .thenReturn(List.of(new JobRequestListDto()));

        PageDto<JobRequestListDto> result = jobRequestService.searchJobRequestsCustomer(searchDto, 0, 10);

        assertFalse(result.getContent().isEmpty());
        verify(jobRequestRepository).searchJobRequestsCustomer(eq(user.getId()), any(), any(), any(), any(), any(), any());
    }

    @Test
    void searchJobRequestsWorker_withDistance_shouldCallDistanceSearch() {

        searchDto.setDistance(50); // distance triggers the specific repository call
        when(userService.getCurrentUser()).thenReturn(worker);
        when(licenseService.hasApprovedLicense(worker.getId())).thenReturn(true);

        Page<JobRequest> jobRequestPage = new PageImpl<>(List.of(jobRequest), PageRequest.of(0, 10), 1);
        when(jobRequestRepository.searchOpenJobRequestsWorkerWithDistance(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        ).thenReturn(jobRequestPage);

        when(jobRequestMapper.jobRequestToListWithMinPriceDto(jobRequest))
            .thenReturn(new JobRequestListWithMinPriceDto());

        var pageDto = jobRequestService.searchJobRequestsWorker(searchDto, 0, 10);

        assertFalse(pageDto.getContent().isEmpty());
        verify(jobRequestRepository).searchOpenJobRequestsWorkerWithDistance(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }


    @Test
    void getByIdWithCustomer_shouldReturnWithCustomerDto() {
        when(jobRequestRepository.findByIdWithCustomer(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));
        when(jobRequestMapper.jobRequestToWithCustomerDto(jobRequest)).thenReturn(new JobRequestWithCustomerDto());

        JobRequestWithCustomerDto result = jobRequestService.getByIdWithCustomer(JOB_REQUEST_ID);

        assertNotNull(result);
    }

    @Test
    void deleteById_whenNotFound_shouldThrowNotFoundException() {
        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> jobRequestService.deleteById(JOB_REQUEST_ID));
    }

    @Test
    void create_withDefaultStatus_shouldSetPending() {
        when(userService.getCurrentUser()).thenReturn(user);
        createDto.setStatus(null);
        when(jobRequestRepository.save(any(JobRequest.class))).thenAnswer(invocation -> {
            JobRequest saved = invocation.getArgument(0);
            assertEquals(JobStatus.PENDING, saved.getStatus());
            return jobRequest;
        });
        when(jobRequestMapper.jobRequestToDetailDto(any())).thenReturn(new JobRequestDetailDto());

        jobRequestService.create(createDto);

        verify(jobRequestRepository).save(any(JobRequest.class));
    }

    @Test
    void update_withPropertyRemoval_shouldSetPropertyNull() {
        updateDto.setPropertyId(null);
        jobRequest.setProperty(new Property());

        when(userService.getCurrentUser()).thenReturn(user);
        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));
        when(jobRequestRepository.save(any())).thenReturn(jobRequest);
        when(jobRequestMapper.jobRequestToUpdateDto(any())).thenReturn(updateDto);

        jobRequestService.update(updateDto);

        assertNull(jobRequest.getProperty());
    }

    @Test
    void findAllOpen_shouldCalculateLowestPrice() {
        ApplicationUser worker = ApplicationUser.builder().id(2L).role(Role.WORKER).build();

        JobOffer offer1 = JobOffer.builder().worker(new ApplicationUser()).price(100.0f).status(JobOfferStatus.PENDING).build();
        JobOffer offer2 = JobOffer.builder().worker(new ApplicationUser()).price(80.0f).status(JobOfferStatus.PENDING).build();
        jobRequest.setReceivedJobOffers(List.of(offer1, offer2));

        when(userService.getCurrentUser()).thenReturn(worker);
        when(licenseService.hasApprovedLicense(worker.getId())).thenReturn(true);
        when(jobRequestRepository.findAllByStatusWithOffers(JobStatus.PENDING)).thenReturn(List.of(jobRequest));
        when(jobRequestMapper.jobRequestToListWithMinPriceDto(any())).thenAnswer(invocation -> {
            JobRequestListWithMinPriceDto dto = new JobRequestListWithMinPriceDto();
            dto.setLowestPrice(80.0f);
            return dto;
        });

        List<JobRequestListWithMinPriceDto> result = jobRequestService.findAllOpen();

        assertEquals(80.0f, result.getFirst().getLowestPrice());
    }

    @Test
    void findAllOpen_shouldReturnEmptyList_whenLicenseNotApproved() {
        ApplicationUser worker = ApplicationUser.builder().id(2L).role(Role.WORKER).build();

        when(userService.getCurrentUser()).thenReturn(worker);
        when(licenseService.hasApprovedLicense(worker.getId())).thenReturn(false);
        List<JobRequestListWithMinPriceDto> result = jobRequestService.findAllOpen();

        assertTrue(result.isEmpty(), "Expected empty list when worker has no approved license");
    }

    @Test
    void update_whenJobRequestIsHidden_throwsNotFoundException() {
        jobRequest.setStatus(JobStatus.HIDDEN);
        when(jobRequestRepository.findById(updateDto.getId())).thenReturn(Optional.of(jobRequest));
        assertThrows(NotFoundException.class, () -> jobRequestService.update(updateDto));
    }

    @Test
    void getById_whenJobRequestIsHidden_throwsNotFoundException() {
        jobRequest.setStatus(JobStatus.HIDDEN);
        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));
        assertThrows(NotFoundException.class, () -> jobRequestService.getById(JOB_REQUEST_ID));
    }

    @Test
    void getByIdWithCustomer_whenJobRequestIsHidden_throwsNotFoundException() {
        jobRequest.setStatus(JobStatus.HIDDEN);
        when(jobRequestRepository.findByIdWithCustomer(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));
        assertThrows(NotFoundException.class, () -> jobRequestService.getByIdWithCustomer(JOB_REQUEST_ID));
    }

    @Test
    void listJobRequests_whenUserNotFoundInRepo_throwsNotFoundException() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(userRepository.findUserByUsername(user.getUsername())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> jobRequestService.listJobRequests());
    }

    @Test
    void deleteById_whenJobIsDone_shouldSetStatusToHidden() {
        jobRequest.setStatus(JobStatus.DONE);
        when(userService.getCurrentUser()).thenReturn(user);
        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));

        jobRequestService.deleteById(JOB_REQUEST_ID);

        assertEquals(JobStatus.HIDDEN, jobRequest.getStatus());
        verify(jobRequestRepository).save(jobRequest);
        verify(jobRequestRepository, never()).delete(any());
    }

    @Test
    void deleteById_whenJobHasReport_shouldSetStatusToHidden() {
        when(reportRepository.existsByJobRequest_Id(JOB_REQUEST_ID)).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(user);
        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));

        jobRequestService.deleteById(JOB_REQUEST_ID);

        assertEquals(JobStatus.HIDDEN, jobRequest.getStatus());
        verify(jobRequestRepository).save(jobRequest);
        verify(jobRequestRepository, never()).delete(any());
    }

    @Test
    void deleteById_whenAlreadyHidden_shouldThrowIllegalStateException() {
        jobRequest.setStatus(JobStatus.HIDDEN);
        when(userService.getCurrentUser()).thenReturn(user);
        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));

        assertThrows(IllegalStateException.class, () -> jobRequestService.deleteById(JOB_REQUEST_ID));
    }

    @Test
    void markRequestDone_whenNoAcceptedOffer_shouldThrowIllegalStateException() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));
        when(jobOfferRepository.findByJobRequestIdAndStatus(JOB_REQUEST_ID, JobOfferStatus.ACCEPTED)).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> jobRequestService.markRequestDone(JOB_REQUEST_ID));
    }

    @Test
    void findAll_shouldReturnListOfPendingJobs() {
        when(jobRequestRepository.findAllByStatusAndCustomerBannedFalse(JobStatus.PENDING)).thenReturn(List.of(jobRequest));
        when(jobRequestMapper.jobRequestToListDto(List.of(jobRequest))).thenReturn(List.of(new JobRequestListDto()));

        List<JobRequestListDto> result = jobRequestService.findAll();

        assertFalse(result.isEmpty());
        verify(jobRequestRepository).findAllByStatusAndCustomerBannedFalse(JobStatus.PENDING);
    }

    @Test
    void searchJobRequestsAdmin_shouldCallAdminSearchAndReturnDtoList() {
        Page<JobRequest> jobRequestPage = new PageImpl<>(List.of(jobRequest));
        when(jobRequestRepository.searchJobRequestsAdmin(any(), any(), any(), any(), any())).thenReturn(jobRequestPage);
        when(jobRequestMapper.jobRequestToListDto(anyList())).thenReturn(List.of(new JobRequestListDto()));

        PageDto<JobRequestListDto> result = jobRequestService.searchJobRequestsAdmin(searchDto, 0, 10);

        assertFalse(result.getContent().isEmpty());
        verify(jobRequestRepository).searchJobRequestsAdmin(eq("Test"), anyList(), any(),  any(LocalDate.class), any(Pageable.class));
    }
}