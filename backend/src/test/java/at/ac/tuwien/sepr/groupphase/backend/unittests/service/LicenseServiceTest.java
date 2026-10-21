package at.ac.tuwien.sepr.groupphase.backend.unittests.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.AdminLicenseListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseDownloadDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.LicenseMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.License;
import at.ac.tuwien.sepr.groupphase.backend.exception.LicenseAlreadyApprovedException;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.LicenseRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.PushNotificationService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.LicenseServiceImpl;
import at.ac.tuwien.sepr.groupphase.backend.type.LicenseStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LicenseServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LicenseRepository licenseRepository;
    @Mock
    private LicenseMapper licenseMapper;
    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private LicenseServiceImpl licenseService;

    private ApplicationUser user;
    private LicenseCreateDto createDto;
    private LicenseUpdateDto updateDto;
    private LicenseDownloadDto downloadDto;
    private LicenseDetailDto detailDto;
    private License license;
    private MultipartFile file;

    @BeforeEach
    void setUp() {
        user = ApplicationUser.builder()
            .id(1L)
            .username("john")
            .email("john@example.com")
            .role(Role.WORKER)
            .build();

        license = License.builder()
            .id(1L)
            .filename("license.pdf")
            .description("Test License")
            .mediaType("application/pdf")
            .file("sample-content".getBytes())
            .worker(user)
            .status(LicenseStatus.PENDING)
            .build();

        file = new MockMultipartFile(
            "license",
            "license.pdf",
            "application/pdf",
            "sample-content".getBytes()
        );

        createDto = LicenseCreateDto.builder()
            .filename("license.pdf")
            .description("Test License")
            .mediaType("application/pdf")
            .build();

        updateDto = LicenseUpdateDto.builder()
            .id(1L)
            .filename("license_updated.pdf")
            .description("Updated description")
            .mediaType("application/pdf")
            .build();

        downloadDto = LicenseDownloadDto.builder()
            .filename("license.pdf")
            .file("sample-content".getBytes())
            .mediaType("application/pdf")
            .build();

        detailDto = LicenseDetailDto.builder()
            .id(1L)
            .filename("license.pdf")
            .description("Test License Description")
            .status(LicenseStatus.PENDING)
            .uploadTime(LocalDateTime.of(2023, 1, 1, 10, 0))
            .mediaType("application/pdf")
            .build();
    }

    @Test
    void create_withValidData_shouldSaveAndReturnDto() throws IOException {
        when(userService.getCurrentUser()).thenReturn(user);
        when(userRepository.findUserByUsername("john")).thenReturn(Optional.of(user));
        when(licenseRepository.save(any(License.class))).thenReturn(license);
        LicenseCreateDto result = licenseService.create(createDto, file);

        assertNotNull(result);
        assertEquals(createDto, result);
        verify(licenseRepository).save(any(License.class));
    }


    @Test
    void create_withNonExistingUser_shouldThrowNotFoundException() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(userRepository.findUserByUsername(user.getUsername())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> licenseService.create(createDto, file));
    }

    @Test
    void getFileInformationById_withValidId_shouldReturnDetailDto() {
        when(licenseRepository.findById(1L)).thenReturn(Optional.of(license));
        when(licenseMapper.licenseToDetailDto(license)).thenReturn(detailDto);

        LicenseDetailDto result = licenseService.getFileInformationById(1L);

        assertNotNull(result);
        assertEquals(detailDto, result);
        verify(licenseMapper).licenseToDetailDto(license);
    }

    @Test
    void getFileInformationById_withInvalidId_shouldThrowNotFoundException() {
        when(licenseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> licenseService.getFileInformationById(1L));
    }

    @Test
    void listLicenses_withValidUsername_shouldReturnListDto() {
        when(userRepository.findUserByUsername("john")).thenReturn(Optional.of(user));
        when(licenseRepository.findAllByWorkerId(user.getId())).thenReturn(List.of(license));
        when(licenseMapper.licenseToListDto(List.of(license))).thenReturn(List.of(new LicenseListDto()));
        List<LicenseListDto> result = licenseService.listLicenses("john");

        assertEquals(1, result.size());
        verify(licenseMapper).licenseToListDto(anyList());
    }

    @Test
    void listLicenses_withNonExistingUser_shouldThrowNotFoundException() {
        when(userRepository.findUserByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> licenseService.listLicenses("unknown"));
    }

    @Test
    void downloadFileById_withValidId_shouldReturnDownloadDto() {
        when(licenseRepository.findById(1L)).thenReturn(Optional.of(license));
        when(licenseMapper.licenseToDownloadDto(license)).thenReturn(downloadDto);
        LicenseDownloadDto result = licenseService.downloadFileById(1L);

        assertNotNull(result);
        assertEquals(downloadDto, result);
        verify(licenseMapper).licenseToDownloadDto(license);
    }

    @Test
    void updateLicense_withAuthorizedUser_shouldUpdate() throws IOException {
        when(userService.getCurrentUser()).thenReturn(user);
        when(licenseRepository.findById(1L)).thenReturn(Optional.of(license));
        when(licenseRepository.save(any())).thenReturn(license);

        LicenseUpdateDto result = licenseService.update(updateDto, file);
        assertEquals(updateDto, result);
        verify(licenseRepository).save(any(License.class));
    }

    @Test
    void updateLicense_withUnauthorizedUser_shouldThrowAccessDeniedException() {
        ApplicationUser otherUser = ApplicationUser.builder().id(2L).build();
        license.setWorker(otherUser);

        when(userService.getCurrentUser()).thenReturn(user);
        when(licenseRepository.findById(1L)).thenReturn(Optional.of(license));

        assertThrows(AccessDeniedException.class,
            () -> licenseService.update(updateDto, file));
    }

    @Test
    void updateLicense_withNonExistingLicense_shouldThrowNotFoundException() {
        when(licenseRepository.findById(99L)).thenReturn(Optional.empty());
        updateDto.setId(99L);
        assertThrows(NotFoundException.class, () -> licenseService.update(updateDto, file));
    }

    @Test
    void updateLicense_whenAlreadyApproved_shouldThrowUnsupportedOperationException() {
        when(licenseRepository.findById(1L)).thenReturn(Optional.of(license));
        license.setStatus(LicenseStatus.APPROVED);
        assertThrows(LicenseAlreadyApprovedException.class, () -> licenseService.update(updateDto, file));

    }

    @Test
    void deleteLicense_withAuthorizedUser_shouldDelete() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(licenseRepository.findById(1L)).thenReturn(Optional.of(license));

        licenseService.deleteById(1L);
        verify(licenseRepository).deleteById(1L);
    }

    @Test
    void deleteLicense_withUnauthorizedUser_shouldThrowAccessDeniedException() {
        ApplicationUser otherUser = ApplicationUser.builder().id(2L).build();
        license.setWorker(otherUser);

        when(userService.getCurrentUser()).thenReturn(user);
        when(licenseRepository.findById(1L)).thenReturn(Optional.of(license));

        assertThrows(AccessDeniedException.class,
            () -> licenseService.deleteById(1L));
    }

    @Test
    void deleteLicense_withNonExistingLicense_shouldThrowNotFoundException() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(licenseRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> licenseService.deleteById(1L));
    }

    @Test
    void listPendingLicenses_shouldReturnOnlyPending() {
        license.setStatus(LicenseStatus.PENDING);
        when(licenseRepository.findAllByStatus(LicenseStatus.PENDING)).thenReturn(List.of(license));
        when(licenseMapper.licenseToAdminDto(List.of(license))).thenReturn(List.of(new AdminLicenseListDto()));

        List<AdminLicenseListDto> result = licenseService.listPendingLicenses();

        assertEquals(1, result.size());
        verify(licenseRepository).findAllByStatus(LicenseStatus.PENDING);
    }

    @Test
    void listApprovedLicenses_whenNoneExist_shouldReturnEmptyList() {
        when(licenseRepository.findAllByStatus(LicenseStatus.APPROVED)).thenReturn(Collections.emptyList());
        when(licenseMapper.licenseToAdminDto(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<AdminLicenseListDto> result = licenseService.listApprovedLicenses();

        assertTrue(result.isEmpty());
        verify(licenseRepository).findAllByStatus(LicenseStatus.APPROVED);
    }

    @Test
    void listRejectedLicenses_shouldReturnOnlyRejected() {
        License rejectedLicense = License.builder().id(2L).status(LicenseStatus.REJECTED).build();
        when(licenseRepository.findAllByStatus(LicenseStatus.REJECTED)).thenReturn(List.of(rejectedLicense));
        when(licenseMapper.licenseToAdminDto(List.of(rejectedLicense))).thenReturn(List.of(new AdminLicenseListDto()));

        List<AdminLicenseListDto> result = licenseService.listRejectedLicenses();

        assertEquals(1, result.size());
        verify(licenseRepository).findAllByStatus(LicenseStatus.REJECTED);
    }

    @Test
    void updateLicenseStatus_toApproved_shouldChangeStatusAndSave() {
        when(licenseRepository.findById(1L)).thenReturn(Optional.of(license));
        when(licenseRepository.save(any(License.class))).thenReturn(license);

        licenseService.updateLicenseStatus(1L, LicenseStatus.APPROVED);

        verify(licenseRepository).save(any(License.class));
    }

    @Test
    void updateLicenseStatus_withNonExistingLicense_shouldThrowNotFoundException() {
        when(licenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> licenseService.updateLicenseStatus(99L, LicenseStatus.REJECTED));
    }
}