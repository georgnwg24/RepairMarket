package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page.PageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportListDto;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportMessageDto;
import java.util.List;

public interface ReportService {
    /**
     * Creates a new report based on the provided DTO.
     *
     * @param createDto DTO containing report creation data
     * @return the created report as a detail DTO
     */
    ReportDetailDto create(ReportCreateDto createDto);

    /**
     * Creates a new report based on the provided DTO.
     *
     * @param messageDto DTO containing report creation data
     * @return the created report as a detail DTO
     */
    ReportDetailDto create(ReportMessageDto messageDto);


    /**
     * Retrieves all reports in the system.
     *
     * @return list of report list DTOs
     */
    List<ReportListDto> findAll();

    /**
     * Retrieves all reports in the system by theirs status as a page.
     *
     * @return list of report list DTOs
     */
    PageDto<ReportListDto> findAllByStatus(int offset, int limit, boolean status, String username);

    /**
     * Retrieves all reports filed by the current user.
     *
     * @return list of report list DTOs
     */
    List<ReportListDto> findAllByReporter();

    /**
     * Retrieves all reports targeting a specific user.
     *
     * @param targetId the ID of the target user
     * @return list of report list DTOs
     */
    List<ReportListDto> findAllByTarget(Long targetId);

    /**
     * Retrieves a report by id.
     *
     * @param reportId the ID of the report
     * @return report detail DTO
     */
    ReportDetailDto findById(Long reportId);

    /**
     * Closes an open report.
     *
     * @param reportId the ID of the report to close
     */
    void closeReport(Long reportId);

    /**
     * Opens a close report.
     *
     * @param reportId the ID of the report to open
     */
    void openReport(Long reportId);
}
