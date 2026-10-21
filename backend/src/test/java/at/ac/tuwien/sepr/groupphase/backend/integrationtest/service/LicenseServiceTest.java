package at.ac.tuwien.sepr.groupphase.backend.integrationtest.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseDownloadDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.service.LicenseService;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles({"test", "datagen"})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Transactional
public class LicenseServiceTest {

    @Autowired
    private LicenseService licenseService;

    @BeforeEach
    void setupSecurityContext() {
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken("ethan", "12345678");
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void listLicenses_shouldReturnNonEmptyList() {
        List<LicenseListDto> licenses = licenseService.listLicenses("ethan");
        assertNotNull(licenses);
        assertFalse(licenses.isEmpty());
    }

    @Test
    void createLicense_whenValid_shouldReturnCreateDto() throws Exception {
        LicenseCreateDto createDto = new LicenseCreateDto();
        createDto.setFilename("license1.pdf");
        createDto.setDescription("Test license");
        createDto.setMediaType("application/pdf");

        MockMultipartFile file = new MockMultipartFile(
            "license", "license1.pdf", "application/pdf", "Fake content".getBytes(StandardCharsets.UTF_8)
        );

        LicenseCreateDto returned = licenseService.create(createDto, file);

        assertNotNull(returned);
        assertEquals("license1.pdf", returned.getFilename());
    }

    @Test
    void getFileInformationById_whenExisting_shouldReturnDetailDto() {
        List<LicenseListDto> licenses = licenseService.listLicenses("ethan");
        assertFalse(licenses.isEmpty());

        LicenseDetailDto detailDto = licenseService.getFileInformationById(licenses.getFirst().getId());

        assertNotNull(detailDto);
        assertEquals(licenses.getFirst().getFilename(), detailDto.getFilename());
    }

    @Test
    void getFileInformationById_whenNonExisting_shouldThrowNotFoundException() {
        assertThrows(NotFoundException.class, () -> licenseService.getFileInformationById(99999L));
    }

    @Test
    void downloadFileById_whenExisting_shouldReturnDownloadDto() {
        List<LicenseListDto> licenses = licenseService.listLicenses("ethan");
        assertFalse(licenses.isEmpty());

        LicenseDownloadDto downloadDto = licenseService.downloadFileById(licenses.getFirst().getId());

        assertNotNull(downloadDto);
        assertEquals(licenses.getFirst().getFilename(), downloadDto.getFilename());
        assertNotNull(downloadDto.getFile());
    }

    @Test
    void deleteLicense_whenOwner_shouldDelete() {
        List<LicenseListDto> licenses = licenseService.listLicenses("ethan");
        assertFalse(licenses.isEmpty());
        Long idToDelete = licenses.getFirst().getId();

        assertDoesNotThrow(() -> licenseService.deleteById(idToDelete));
        assertThrows(NotFoundException.class, () -> licenseService.getFileInformationById(idToDelete));
    }

    @Test
    void updateLicense_whenNotOwner_shouldThrowAccessDenied() {
        LicenseUpdateDto updateDto = new LicenseUpdateDto();
        updateDto.setId(-102L);
        updateDto.setFilename("unauthorized.pdf");
        updateDto.setDescription("Should not update");

        MockMultipartFile file = new MockMultipartFile(
            "license", "unauthorized.pdf", "application/pdf", "test".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(AccessDeniedException.class, () -> licenseService.update(updateDto, file));
    }

    @Test
    void deleteLicense_whenNotOwner_shouldThrowAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> licenseService.deleteById(-101L));
    }

}
