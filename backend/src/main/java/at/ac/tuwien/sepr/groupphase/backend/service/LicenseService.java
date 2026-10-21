package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.AdminLicenseListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseDownloadDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license.LicenseUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page.PageDto;
import at.ac.tuwien.sepr.groupphase.backend.type.LicenseStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Service interface for managing licenses.
 * Provides methods for creating, retrieving, updating, downloading, listing, and deleting license data.
 */
public interface LicenseService {

    /**
     * Creates a new license with the given data and license file.
     *
     * @param createDto the DTO containing license metadata
     * @param license the uploaded license file
     * @return the created license as a LicenseCreateDto
     * @throws IOException if there is an error processing the file
     */
    LicenseCreateDto create(LicenseCreateDto createDto, MultipartFile license) throws IOException;

    /**
     * Retrieves detailed information about a license file by its ID.
     *
     * @param id the ID of the license
     * @return a LicenseDetailDto containing the license's details
     */
    LicenseDetailDto getFileInformationById(Long id);

    /**
     * Retrieves a list of all licenses stored in the system.
     *
     * @return a list representing all licenses
     */
    List<LicenseListDto> listLicenses(String username);

    /**
     * Downloads the license file identified by its ID.
     *
     * @param id the ID of the license to download
     * @return a LicenseDownloadDto containing the license file data
     */
    LicenseDownloadDto downloadFileById(Long id);

    /**
     * Updates an existing license with the provided data and file.
     *
     * @param licenseUpdateDto the DTO containing updated license data
     * @param license the new license file
     * @return the updated license as a LicenseUpdateDto
     * @throws IOException if there is an error processing the file
     */
    LicenseUpdateDto update(LicenseUpdateDto licenseUpdateDto, MultipartFile license) throws IOException;

    /**
     * Deletes a license identified by its ID.
     *
     * @param id the ID of the license to delete
     */
    void deleteById(Long id);

    /**
     * Retrieves a list of all licenses whose status is PENDING.
     */
    List<AdminLicenseListDto> listPendingLicenses();

    /**
     * Retrieves a list of all licenses whose status is PENDING as page.
     */
    PageDto<AdminLicenseListDto> listLicensesPageByStatus(int offset, int limit, LicenseStatus status, String username);

    /**
     * Retrieves a list of all licenses whose status is APPROVED.
     */
    List<AdminLicenseListDto> listApprovedLicenses();

    /**
     * Retrieves a list of all licenses whose status is REJECTED.
     */
    List<AdminLicenseListDto> listRejectedLicenses();


    /**
     * Updates the status of the license identified by id.
     *
     * @param id      the ID of the license to update
     * @param status  the new status to apply
     * @return        the updated license details
     */
    LicenseDetailDto updateLicenseStatus(Long id, LicenseStatus status);

    /**
     * find if the worker has an APPROVED license.
     *
     * @return true if the given user has an APPROVED license
     */
    public boolean hasApprovedLicense(Long workerId);
}
