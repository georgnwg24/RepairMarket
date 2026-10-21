package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page.PageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportMessageDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.service.ReportService;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.lang.invoke.MethodHandles;
import java.util.List;


@Validated
@RestController
@RequestMapping(path = ReportEndpoint.BASE_PATH)
public class ReportEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final String BASE_PATH = "/api/v1/reports";
    private final ReportService reportService;

    @Autowired
    public ReportEndpoint(ReportService reportService) {
        this.reportService = reportService;
    }

    @PermitAll
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ReportDetailDto> createReport(@RequestBody @Valid ReportCreateDto createDto) {
        LOGGER.info("POST {} - create report: {}", BASE_PATH, createDto);
        try {
            ReportDetailDto detail = reportService.create(createDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(detail);
        } catch (Exception e) {
            LOGGER.error("Error creating report", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PermitAll
    @GetMapping("/me")
    public ResponseEntity<List<ReportListDto>> getMyReports() {
        LOGGER.info("GET " + BASE_PATH + "/me");
        return ResponseEntity.ok(reportService.findAllByReporter());
    }

    /*
    @PermitAll
    @GetMapping
    public ResponseEntity<List<ReportListDto>> getAllReports(
    ) {
        LOGGER.info("GET {}", BASE_PATH);
        return ResponseEntity.ok(reportService.findAll());
    }

     */


    @RolesAllowed("ADMIN")
    @GetMapping
    public ResponseEntity<PageDto<ReportListDto>> getAllReports(
        @RequestParam(defaultValue = "0") int offset,
        @RequestParam(defaultValue = "10") int limit,
        @RequestParam(defaultValue = "true") boolean status,
        @RequestParam(value = "username", required = false) String username
    ) {
        LOGGER.info("GET {}", BASE_PATH);
        return ResponseEntity.ok(reportService.findAllByStatus(offset, limit, status, username));
    }



    @PermitAll
    @GetMapping("/target/{targetId}")
    public ResponseEntity<List<ReportListDto>> getReportsByTarget(@PathVariable Long targetId) {
        LOGGER.info("GET {} /target/{}", BASE_PATH, targetId);
        try {
            return ResponseEntity.ok(reportService.findAllByTarget(targetId));
        } catch (NotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @RolesAllowed("ADMIN")
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportDetailDto> getReportDetail(@PathVariable Long reportId) {
        LOGGER.info("GET {}/{}", BASE_PATH, reportId);
        try {
            ReportDetailDto dto = reportService.findById(reportId);
            return ResponseEntity.ok(dto);
        } catch (NotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @RolesAllowed("ADMIN")
    @PostMapping("/{reportId}/close")
    public ResponseEntity<Void> closeReport(@PathVariable Long reportId) {
        LOGGER.info("POST {} /{}/close", BASE_PATH, reportId);
        try {
            reportService.closeReport(reportId);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @RolesAllowed("ADMIN")
    @PostMapping("/{reportId}/open")
    public ResponseEntity<Void> openReport(@PathVariable Long reportId) {
        LOGGER.info("POST {} /{}/open", BASE_PATH, reportId);
        try {
            reportService.openReport(reportId);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @RolesAllowed({"WORKER", "CUSTOMER"})
    @PostMapping("/messages")
    public ResponseEntity<ReportDetailDto> reportMessage(
        @Valid @RequestBody ReportMessageDto reportMessageDto,
        BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }

        ReportDetailDto created = reportService.create(reportMessageDto);

        return new ResponseEntity<>(created,  HttpStatus.CREATED);
    }

}
