package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.AdminLicenseListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseDownloadDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page.PageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.LicenseMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.License;
import at.ac.tuwien.sepr.groupphase.backend.exception.LicenseAlreadyApprovedException;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.LicenseRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.LicenseService;
import at.ac.tuwien.sepr.groupphase.backend.service.PushNotificationService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.type.LicenseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

@Service
public class LicenseServiceImpl implements LicenseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final UserService userService;
    private final UserRepository userRepository;
    private final LicenseRepository licenseRepository;
    private final LicenseMapper licenseMapper;
    private final PushNotificationService pushNotificationService;


    public LicenseServiceImpl(UserService userService, LicenseRepository licenseRepository, LicenseMapper licenseMapper, UserRepository userRepository, PushNotificationService pushNotificationService) {
        this.userService = userService;
        this.licenseRepository = licenseRepository;
        this.licenseMapper = licenseMapper;
        this.userRepository = userRepository;
        this.pushNotificationService = pushNotificationService;
    }


    @Override
    public LicenseCreateDto create(LicenseCreateDto createDto, MultipartFile license) throws IOException {
        LOGGER.trace("create() with parameters: {} {}", createDto, license);
        String username = userService.getCurrentUser().getUsername();
        ApplicationUser user = userRepository.findUserByUsername(username).orElseThrow(() -> {
            LOGGER.error("Error while fetching: {}", username);
            return new NotFoundException();
        });

        License licenseInfo = License.builder()
            .filename(createDto.getFilename())
            .description(createDto.getDescription())
            .worker(user)
            .mediaType(createDto.getMediaType())
            .file(license.getBytes())
            .build();

        License saved = licenseRepository.save(licenseInfo);

        // send a push notification
        pushNotificationService.notifyAdminsOfLicense(saved.getId(), false);

        return createDto;
    }

    @Override
    public LicenseDetailDto getFileInformationById(Long id) {
        LOGGER.trace("getFileInformationById() with parameters: {}", id);
        return licenseMapper.licenseToDetailDto(licenseRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("License information not found")));
    }


    @Override
    public List<LicenseListDto> listLicenses(String username) {
        LOGGER.trace("listLicenses() with parameters: {}", username);
        ApplicationUser user = userRepository.findUserByUsername(username).orElseThrow(() -> {
            LOGGER.error("Error while fetching User: {}", username);
            return new NotFoundException();
        });
        Long id = user.getId();

        return licenseMapper.licenseToListDto(licenseRepository.findAllByWorkerId(id));

    }

    @Override
    public LicenseDownloadDto downloadFileById(Long id) {
        LOGGER.trace("downloadFileById() with parameters: {}", id);
        return licenseMapper.licenseToDownloadDto(licenseRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("License information not found")));
    }

    @Override
    public LicenseUpdateDto update(LicenseUpdateDto licenseUpdateDto, MultipartFile license) throws IOException {
        LOGGER.trace("update() with parameters: {}, {}", licenseUpdateDto, license);
        License licenseInfo = licenseRepository.findById(licenseUpdateDto.getId())
            .orElseThrow(() -> {
                LOGGER.error("User not found for username: {}", licenseUpdateDto.getId());
                return new NotFoundException();
            });

        if (licenseInfo.getStatus().equals(LicenseStatus.APPROVED)) {
            LOGGER.error("License status is APPROVED");
            throw new LicenseAlreadyApprovedException();
        }

        if (!licenseInfo.getWorker().getId().equals(userService.getCurrentUser().getId())) {
            LOGGER.error("User '{}' is not authorized to update License with id{}", licenseInfo.getWorker().getUsername(), licenseUpdateDto.getId());
            throw new AccessDeniedException("Not Authorized");
        }
        licenseMapper.updateLicenseFromDto(licenseUpdateDto, licenseInfo);
        licenseInfo.setFile(license.getBytes());
        licenseRepository.save(licenseInfo);

        // send a push notification
        pushNotificationService.notifyAdminsOfLicense(licenseInfo.getId(), true);

        return licenseUpdateDto;
    }

    @Override
    public void deleteById(Long id) {
        ApplicationUser user = userService.getCurrentUser();
        LOGGER.trace("deleteById() with parameters: {}", id);
        ApplicationUser authorizedUser = licenseRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("No user with id: " + id))
            .getWorker();

        if (!user.getId().equals(authorizedUser.getId())) {
            LOGGER.error("You are not Authorized");
            throw new AccessDeniedException("You are not Authorized");
        }

        licenseRepository.deleteById(id);
    }

    @Override
    public List<AdminLicenseListDto> listPendingLicenses() {
        LOGGER.trace("listPendingLicenses()");
        List<License> pending = licenseRepository.findAllByStatus(LicenseStatus.PENDING);
        return licenseMapper.licenseToAdminDto(pending);
    }

    @Override
    public PageDto<AdminLicenseListDto> listLicensesPageByStatus(int offset, int limit, LicenseStatus status, String username) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<License> licensePage;

        if (username == null || username.isBlank()) {
            licensePage = licenseRepository.findAllByStatus(status, pageable);
        } else {
            licensePage = licenseRepository.findAllByStatusAndWorker_UsernameContainingIgnoreCase(status, username, pageable);
        }
        List<AdminLicenseListDto> licenseList = licenseMapper.licenseToAdminDto(licensePage.getContent());
        return new PageDto<>(licenseList, (int) licensePage.getTotalElements(), limit, offset);
    }

    @Override
    public List<AdminLicenseListDto> listApprovedLicenses() {
        LOGGER.trace("listApprovedLicenses()");
        List<License> pending = licenseRepository.findAllByStatus(LicenseStatus.APPROVED);
        return licenseMapper.licenseToAdminDto(pending);
    }


    @Override
    public List<AdminLicenseListDto> listRejectedLicenses() {
        LOGGER.trace("listRejectedLicenses()");
        List<License> pending = licenseRepository.findAllByStatus(LicenseStatus.REJECTED);
        return licenseMapper.licenseToAdminDto(pending);
    }


    @Override
    public LicenseDetailDto updateLicenseStatus(Long id, LicenseStatus status) {
        LOGGER.trace("updateLicenseStatus() with parameters: {} {}", id, status);
        License lic = licenseRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("License not found: " + id));

        lic.setStatus(status);
        License saved = licenseRepository.save(lic);

        // send a push notification
        if (status == LicenseStatus.APPROVED) {
            pushNotificationService.notifyWorkerOfLicenseApproved(lic.getWorker());
        } else if (status == LicenseStatus.REJECTED) {
            pushNotificationService.notifyWorkerOfLicenseRejected(lic.getWorker());
        }

        return licenseMapper.licenseToDetailDto(saved);
    }

    @Override
    public boolean hasApprovedLicense(Long workerId) {
        return licenseRepository.existsByWorkerIdAndStatus(workerId, LicenseStatus.APPROVED);
    }
}
