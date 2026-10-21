package at.ac.tuwien.sepr.groupphase.backend.service.impl;

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
import at.ac.tuwien.sepr.groupphase.backend.service.JobRequestService;
import at.ac.tuwien.sepr.groupphase.backend.service.LicenseService;
import at.ac.tuwien.sepr.groupphase.backend.service.PushNotificationService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.type.Category;
import at.ac.tuwien.sepr.groupphase.backend.type.JobOfferStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.JobStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static at.ac.tuwien.sepr.groupphase.backend.type.JobStatus.PENDING;


@Service
public class JobRequestServiceImpl implements JobRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final JobRequestRepository jobRequestRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final JobRequestMapper jobRequestMapper;
    private final UserService userService;
    private final JobOfferRepository jobOfferRepository;
    private final LicenseService licenseService;
    private final PushNotificationService pushNotificationService;
    private final ReportRepository reportRepository;

    @Autowired
    public JobRequestServiceImpl(JobRequestRepository jobRequestRepository, PropertyRepository propertyRepository, UserRepository userRepository,
                                 JobRequestMapper jobRequestMapper, UserService userService, JobOfferRepository jobOfferRepository, LicenseService licenseService, PushNotificationService pushNotificationService,
                                 ReportRepository reportRepository) {
        this.jobRequestRepository = jobRequestRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.jobRequestMapper = jobRequestMapper;
        this.userService = userService;
        this.jobOfferRepository = jobOfferRepository;
        this.licenseService = licenseService;
        this.pushNotificationService = pushNotificationService;
        this.reportRepository = reportRepository;
    }

    @Override
    public JobRequestDetailDto create(JobRequestCreateDto createDto) throws ConstraintViolationException {
        LOGGER.trace("create() with parameters: {}", createDto);
        ApplicationUser user = userService.getCurrentUser();
        JobRequest jobRequest = new JobRequest();
        jobRequest.setTitle(createDto.getTitle());
        jobRequest.setDescription(createDto.getDescription());
        jobRequest.setCategory(createDto.getCategory());
        jobRequest.setStatus(createDto.getStatus() != null ? createDto.getStatus() : PENDING);
        jobRequest.setDeadline(createDto.getDeadline());
        jobRequest.setCustomer(user);

        if (createDto.getPropertyId() != null) {
            Property property = propertyRepository.findById(createDto.getPropertyId())
                .orElseThrow(() -> {
                    LOGGER.error("Property with ID {} not found", createDto.getPropertyId());
                    return new NotFoundException("Property not found");
                });
            jobRequest.setProperty(property);
        }

        JobRequest saved = jobRequestRepository.save(jobRequest);

        // send push notifications
        pushNotificationService.notifyWorkersOfJobRequest(saved);
        return jobRequestMapper.jobRequestToDetailDto(saved);
    }


    @Override
    public JobRequestUpdateDto update(JobRequestUpdateDto updateDto) throws NotFoundException, ConstraintViolationException {
        ApplicationUser user = userService.getCurrentUser();
        LOGGER.trace("update() with parameters: {}", updateDto);
        JobRequest jobRequest = jobRequestRepository.findById(updateDto.getId()).orElseThrow(() -> {
            LOGGER.error("Error while updating JobRequest: {}", updateDto);
            return new NotFoundException();
        });

        if (jobRequest.getStatus() == JobStatus.HIDDEN) {
            throw new NotFoundException("JobRequest was already deleted");
        }

        if (!jobRequest.getCustomer().getId().equals(user.getId())) {
            LOGGER.error("User '{}' is not authorized to update JobRequest with id {}", user.getUsername(), updateDto.getId());
            throw new AccessDeniedException("You are not Authorized");
        }
        jobRequestMapper.updateJobRequestFromDto(updateDto, jobRequest);
        if (updateDto.getPropertyId() != null) {
            Property property = propertyRepository.findById(updateDto.getPropertyId()).orElseThrow(() -> {
                LOGGER.error("Error while updating JobRequest: {}", updateDto);
                return new NotFoundException();
            });
            jobRequest.setProperty(property);
        } else {
            jobRequest.setProperty(null);
        }
        jobRequestRepository.save(jobRequest);
        return jobRequestMapper.jobRequestToUpdateDto(jobRequest);
    }


    @Override
    public JobRequestDetailDto getById(long id) throws NotFoundException {
        LOGGER.trace("getById() with parameters: {}", id);

        JobRequest jobRequest = jobRequestRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("JobRequest couldn't be found"));

        if (jobRequest.getStatus() == JobStatus.HIDDEN) {
            throw new NotFoundException("JobRequest was already deleted");
        }

        return jobRequestMapper.jobRequestToDetailDto(jobRequest);
    }

    @Override
    public JobRequestWithCustomerDto getByIdWithCustomer(long id) throws NotFoundException {
        LOGGER.trace("getByIdWithCustomer() with ID: {}", id);

        JobRequest jobRequest = jobRequestRepository.findByIdWithCustomer(id).orElseThrow(() -> {
            LOGGER.warn("JobRequest with ID {} not found", id);
            return new NotFoundException("JobRequest couldn't be found");
        });
        if (jobRequest.getStatus() == JobStatus.HIDDEN) {
            throw new NotFoundException("JobRequest was already deleted");
        }

        return jobRequestMapper.jobRequestToWithCustomerDto(jobRequest);
    }

    @Override
    public List<JobRequestListDto> listJobRequests() {
        String username = userService.getCurrentUser().getUsername();
        LOGGER.trace("listJobRequests() with parameters: {}", username);
        ApplicationUser user = userRepository.findUserByUsername(username).orElseThrow(() -> {
            LOGGER.error("Error while fetching Job Request: {}", username);
            return new NotFoundException("User not found");
        });
        Long id = user.getId();
        return jobRequestMapper.jobRequestToListDto(
            jobRequestRepository.findAllByCustomerIdAndCustomerBannedFalse(id).stream()
                .filter(request -> request.getStatus() != JobStatus.HIDDEN)
                .toList());

    }

    @Override
    public void deleteById(long id) throws NotFoundException {
        LOGGER.trace("deleteById({})", id);
        ApplicationUser user = userService.getCurrentUser();
        ApplicationUser deletingUser = jobRequestRepository.findById(id).orElseThrow(() -> {
            LOGGER.error("Error while deleting Job Request: {}", id);
            return new NotFoundException();
        }).getCustomer();

        boolean isOwner = deletingUser.getId().equals(user.getId());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(granted -> granted.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            LOGGER.error("User '{}' is not authorized to delete JobRequest with id {}", user.getUsername(), id);
            throw new AccessDeniedException("You are not Authorized");
        }
        JobRequest jobRequest = jobRequestRepository.findById(id).orElseThrow(() -> new NotFoundException("JobRequest not found"));

        if (jobRequest.getStatus() == JobStatus.DONE || reportRepository.existsByJobRequest_Id(id)) {
            jobRequest.setStatus(JobStatus.HIDDEN);
            jobRequestRepository.save(jobRequest);
        } else if (jobRequest.getStatus() == JobStatus.HIDDEN) {
            throw new IllegalStateException("jobRequest was already deleted");
        } else {
            jobRequestRepository.delete(jobRequest);
        }
    }

    @Override
    public List<JobRequestListDto> findAll() {
        LOGGER.trace("findAll()");
        return jobRequestMapper.jobRequestToListDto(jobRequestRepository.findAllByStatusAndCustomerBannedFalse(PENDING));
    }

    @Override
    public List<JobRequestListWithMinPriceDto> findAllOpen() {
        LOGGER.trace("findAllOpen()");
        ApplicationUser worker = userService.getCurrentUser();

        if (worker.getRole() == Role.WORKER && !licenseService.hasApprovedLicense(worker.getId())) {
            return Collections.emptyList();
        }

        List<JobRequest> openRequests = jobRequestRepository.findAllByStatusWithOffers(PENDING);

        // filter those with PENDING offer from worker out
        return getJobRequestListWithMinPriceDtos(worker, openRequests);
    }

    private Float findLowestPrice(JobRequest jobRequest) {
        return jobRequest.getReceivedJobOffers().stream()
            .filter(offer -> offer.getStatus() == JobOfferStatus.PENDING)
            .map(JobOffer::getPrice)
            .min(Float::compare)
            .orElse(0.0f);
    }


    @Override
    public void markRequestDone(Long id) throws NotFoundException {
        LOGGER.trace("markRequestDone({})", id);
        ApplicationUser currentUser = userService.getCurrentUser();

        JobRequest jobRequest = jobRequestRepository.findById(id)
            .orElseThrow(() -> {
                LOGGER.warn("JobRequest with ID {} not found", id);
                return new NotFoundException("JobRequest not found");
            });

        if (jobRequest.getStatus() == JobStatus.HIDDEN) {
            throw new NotFoundException("JobRequest was already deleted");
        }

        if (!jobRequest.getCustomer().getId().equals(currentUser.getId())) {
            LOGGER.warn("User {} is not the owner of JobRequest {}", currentUser.getId(), id);
            throw new AccessDeniedException("You are not authorized to complete this job request");
        }

        // also mark corresponding offer
        JobOffer acceptedOffer = jobOfferRepository.findByJobRequestIdAndStatus(jobRequest.getId(), JobOfferStatus.ACCEPTED);
        if (acceptedOffer == null) {
            throw new IllegalStateException("No accepted offer found for JobRequest " + jobRequest.getId());
        }

        jobRequest.setStatus(JobStatus.DONE);
        acceptedOffer.setStatus(JobOfferStatus.DONE);

        jobOfferRepository.save(acceptedOffer);
        jobRequestRepository.save(jobRequest);
    }

    @Override
    public PageDto<JobRequestListDto> searchJobRequestsCustomer(JobRequestSearchDto searchDto, int offset, int limit) {
        ApplicationUser user = userService.getCurrentUser();
        Pageable pageable = PageRequest.of(offset / limit, limit);

        List<Category> categories = parseCategories(searchDto.getCategory());
        List<JobStatus> statuses = parseStatuses(searchDto.getStatus());
        Page<JobRequest> jobRequestPage = jobRequestRepository.searchJobRequestsCustomer(
            user.getId(),
            searchDto.getTitle(),
            categories,
            statuses,
            searchDto.getDeadline(),
            searchDto.getPropertyId(),
            pageable
        );
        List<JobRequestListDto> jobRequestListDtos = jobRequestMapper.jobRequestToListDto(jobRequestPage.getContent());
        return new PageDto<>(jobRequestListDtos, (int) jobRequestPage.getTotalElements(), limit, offset);

    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<JobRequestListWithMinPriceDto> searchJobRequestsWorker(JobRequestSearchDto searchDto, int offset, int limit) {
        LOGGER.trace("searchJobRequestsWorker() with parameters: {}", searchDto);

        ApplicationUser worker = userService.getCurrentUser();

        if (!licenseService.hasApprovedLicense(worker.getId())) {
            return new PageDto<>(Collections.emptyList(), 0, 0, 0);
        }

        List<String> categoryNames = (searchDto.getCategory() != null && !searchDto.getCategory().isEmpty())
            ? Arrays.asList(searchDto.getCategory().split(","))
            : null;

        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "id"));

        Page<JobRequest> jobRequestPage = jobRequestRepository.searchOpenJobRequestsWorkerWithDistance(
            searchDto.getTitle(),
            categoryNames,
            searchDto.getDeadline(),
            worker.getLatitude(),
            worker.getLongitude(),
            searchDto.getDistance(),
            searchDto.getLowestPriceMin(),
            searchDto.getLowestPriceMax(),
            worker.getId(),
            pageable
        );

        List<JobRequestListWithMinPriceDto> jobRequestListDtos = jobRequestPage.getContent().stream()
            .map(request -> {
                JobRequestListWithMinPriceDto dto = jobRequestMapper.jobRequestToListWithMinPriceDto(request);
                dto.setLowestPrice(findLowestPrice(request));
                return dto;
            })
            .toList();

        return new PageDto<>(jobRequestListDtos, (int) jobRequestPage.getTotalElements(), limit, offset);
    }

    private List<JobRequestListWithMinPriceDto> getJobRequestListWithMinPriceDtos(ApplicationUser worker, List<JobRequest> openRequests) {
        return openRequests.stream()
            .filter(request -> request.getReceivedJobOffers().stream()
                .noneMatch(offer -> offer.getWorker().equals(worker) && offer.getStatus() == JobOfferStatus.PENDING))
            .map(request -> {
                JobRequestListWithMinPriceDto dto = jobRequestMapper.jobRequestToListWithMinPriceDto(request);
                dto.setLowestPrice(findLowestPrice(request));
                return dto;
            })
            .toList();
    }

    @Override
    public PageDto<JobRequestListDto> searchJobRequestsAdmin(JobRequestSearchDto searchDto, int offset, int limit) {
        LOGGER.trace("searchJobRequestsAdmin() with parameters: {}", searchDto);
        List<Category> categories = parseCategories(searchDto.getCategory());
        List<JobStatus> statuses = parseStatuses(searchDto.getStatus());
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "id"));

        Page<JobRequest> jobRequestPage = jobRequestRepository.searchJobRequestsAdmin(
            searchDto.getTitle(),
            categories,
            statuses,
            searchDto.getDeadline(),
            pageable
        );

        List<JobRequestListDto> jobRequestListDtos = jobRequestMapper.jobRequestToListDto(jobRequestPage.getContent());
        return new PageDto<>(jobRequestListDtos, (int) jobRequestPage.getTotalElements(), limit, offset);

    }

    private List<Category> parseCategories(String categoryStr) {
        if (categoryStr == null || categoryStr.trim().isEmpty()) {
            return null;
        }
        return Arrays.stream(categoryStr.split(","))
            .map(String::trim)
            .map(String::toUpperCase)
            .map(Category::valueOf)
            .collect(Collectors.toList());
    }

    private List<JobStatus> parseStatuses(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return null;
        }
        return Arrays.stream(statusStr.split(","))
            .map(String::trim)
            .map(String::toUpperCase)
            .map(JobStatus::valueOf)
            .collect(Collectors.toList());
    }
}
