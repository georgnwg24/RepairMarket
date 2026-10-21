package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.AdminLicenseListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseDownloadDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseUpdateRestDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page.PageDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.service.LicenseService;
import at.ac.tuwien.sepr.groupphase.backend.type.LicenseStatus;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = LicenseEndpoint.BASE_PATH)
public class LicenseEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final String BASE_PATH = "/api/v1/licenses";
    private final LicenseService licenseService;

    @Autowired
    public LicenseEndpoint(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @PermitAll
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> create(
        @Valid @RequestPart("certificateInfo") LicenseCreateDto toCreate,
        BindingResult bindingResult,
        @RequestPart("certificate") MultipartFile certificate
    ) throws IOException {
        LOGGER.info("POST " + BASE_PATH + "/{}, {}", toCreate, certificate);
        LOGGER.debug("Received LicenseCreateDto: {}", toCreate);

        List<ObjectError> allErrors = new ArrayList<>(bindingResult.getAllErrors());

        if (certificate == null || certificate.isEmpty()) {
            allErrors.add(new ObjectError("certificate", "Certificate file must not be empty"));
        } else {
            String contentType = certificate.getContentType();
            toCreate.setMediaType(contentType);
            if (!isAllowedContentType(contentType)) {
                allErrors.add(new ObjectError("certificate", "Only PDF and image files (PNG, JPG, JPEG) are allowed"));
            }
            if (certificate.getSize() > 5 * 1024 * 1024) {
                allErrors.add(new ObjectError("certificate", "File size exceeds the 5MB limit"));
            }
        }

        if (!allErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(allErrors);
        }

        if (toCreate.getFilename().isEmpty()) {
            toCreate.setFilename(certificate.getOriginalFilename());
        }
        LOGGER.info("POST " + BASE_PATH);
        LOGGER.debug("Body of request:\n{}", toCreate);

        LicenseCreateDto created = this.licenseService.create(toCreate, certificate);
        return ResponseEntity.ok(created);
    }

    @PermitAll
    @PutMapping(path = "/{id}/edit", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> update(
        @Valid @RequestPart("certificateInfo") LicenseUpdateRestDto toUpdate,
        BindingResult bindingResult,
        @RequestPart("certificate") MultipartFile certificate,
        @PathVariable("id") Long id
    ) throws IOException {
        LOGGER.info("PUT " + BASE_PATH + "/{}, {}, {}", toUpdate, certificate, id);
        LOGGER.debug("Received LicenseUpdateRestDto: {}", toUpdate);

        List<ObjectError> allErrors = new ArrayList<>(bindingResult.getAllErrors());

        if (certificate == null || certificate.isEmpty()) {
            allErrors.add(new ObjectError("certificate", "Certificate file must not be empty"));
        } else {
            String contentType = certificate.getContentType();
            toUpdate.setMediaType(contentType);
            if (!isAllowedContentType(contentType)) {
                allErrors.add(new ObjectError("certificate", "Only PDF and image files (PNG, JPG, JPEG) are allowed"));
            }
            if (certificate.getSize() > 5 * 1024 * 1024) {
                allErrors.add(new ObjectError("certificate", "File size exceeds the 5MB limit"));
            }
        }

        if (!allErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(allErrors);
        }

        if (toUpdate.getFilename().isEmpty()) {
            toUpdate.setFilename(certificate.getOriginalFilename());
        }
        LOGGER.info("POST " + BASE_PATH);
        LOGGER.debug("Body of request:\n{}", toUpdate);

        LicenseUpdateDto updated = this.licenseService.update(toUpdate.toUpdateWithId(id), certificate);
        return ResponseEntity.ok(updated);
    }

    private boolean isAllowedContentType(String contentType) {
        return contentType != null && (
            contentType.equals("application/pdf")
                || contentType.equals("image/png")
                || contentType.equals("image/jpeg"));


    }

    @PermitAll
    @GetMapping("/{id}")
    public ResponseEntity<LicenseDetailDto> getLicense(@PathVariable Long id) {
        LOGGER.info("GET " + BASE_PATH + "worker/{}", id);
        return ResponseEntity.ok(licenseService.getFileInformationById(id));
    }

    @PermitAll
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getLicenseFile(@PathVariable Long id) {

        LicenseDownloadDto license = licenseService.downloadFileById(id);
        if (license == null || license.getFile() == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(license.getMediaType()));
        headers.setContentDispositionFormData("attachment", license.getFilename());

        return new ResponseEntity<>(license.getFile(), headers, HttpStatus.OK);
    }

    @PermitAll
    @DeleteMapping(path = "/{id}")
    public ResponseEntity<?> deleteById(
        @PathVariable("id") Long id
    ) throws NotFoundException {
        LOGGER.info("DELETE " + BASE_PATH + "/{}", id);
        licenseService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PermitAll
    @GetMapping(path = "user/{username}")
    public List<LicenseListDto> getWorkerLicenses(
        @PathVariable("username") String username
    ) throws NotFoundException {
        LOGGER.info("GET " + BASE_PATH);
        return licenseService.listLicenses(username);
    }

    @RolesAllowed("ADMIN")
    @GetMapping(path = "/pending")
    public ResponseEntity<PageDto<AdminLicenseListDto>> listPendingLicenses(
        @RequestParam(defaultValue = "0") int offset,
        @RequestParam(defaultValue = "10") int limit,
        @RequestParam(value = "username", required = false) String username
    ) {
        LOGGER.info("GET " + BASE_PATH + "/pending");
        return ResponseEntity.ok(licenseService.listLicensesPageByStatus(offset, limit, LicenseStatus.PENDING, username));
    }

    @RolesAllowed("ADMIN")
    @GetMapping(path = "/approved")
    public ResponseEntity<PageDto<AdminLicenseListDto>> listApprovedLicenses(
        @RequestParam(defaultValue = "0") int offset,
        @RequestParam(defaultValue = "10") int limit,
        @RequestParam(value = "username", required = false) String username
    ) {
        LOGGER.info("GET " + BASE_PATH + "/approved");
        return ResponseEntity.ok(licenseService.listLicensesPageByStatus(offset, limit, LicenseStatus.APPROVED, username));
    }

    @RolesAllowed("ADMIN")
    @GetMapping(path = "/rejected")
    public PageDto<AdminLicenseListDto> listRejectedLicenses(
        @RequestParam(defaultValue = "0") int offset,
        @RequestParam(defaultValue = "10") int limit,
        @RequestParam(value = "username", required = false) String username
    ) {
        LOGGER.info("GET " + BASE_PATH + "/rejected");
        return licenseService.listLicensesPageByStatus(offset, limit, LicenseStatus.REJECTED, username);
    }

    @RolesAllowed("ADMIN")
    @PatchMapping(path = "/{id}/status")
    public ResponseEntity<LicenseDetailDto> updateLicenseStatus(
        @PathVariable("id") Long id,
        @RequestParam("status") LicenseStatus status) {
        LOGGER.info("PATCH " + BASE_PATH + "/{}/status, status={}", id, status);
        LicenseDetailDto updated = licenseService.updateLicenseStatus(id, status);
        return ResponseEntity.ok(updated);
    }
}