package at.ac.tuwien.sepr.groupphase.backend.unittests.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportMessageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.ReportMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.ChatMessage;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.entity.Report;
import at.ac.tuwien.sepr.groupphase.backend.exception.AlreadyReportedException;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.ChatMessageRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReportRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.ChatService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.ReportServiceImpl;
import at.ac.tuwien.sepr.groupphase.backend.type.ReportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReportMapper reportMapper;
    @Mock
    private UserService userService;
    @Mock
    private JobRequestRepository jobRequestRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private ApplicationUser reporter;
    private ApplicationUser target;
    private JobRequest jobRequest;
    private Report report;
    private ReportCreateDto createDto;
    private Pageable pageable;
    private ChatService chatService;
    private ChatMessageRepository chatMessageRepository;
    private ChatMessage chatMessage;

    @BeforeEach
    void setUp() {
        reporter = ApplicationUser.builder()
            .id(1L)
            .username("reporterUser")
            .build();

        target = ApplicationUser.builder()
            .id(2L)
            .username("targetUser")
            .build();

        jobRequest = JobRequest.builder()
            .id(1L)
            .customer(target)
            .build();

        createDto = ReportCreateDto.builder()
            .jobRequestId(1L)
            .type(ReportType.JOB_REQUEST)
            .reason("Inappropriate behavior")
            .build();

        report = Report.builder()
            .id(1L)
            .reporter(reporter)
            .target(target)
            .jobRequest(jobRequest)
            .type(ReportType.JOB_REQUEST)
            .reason("Inappropriate behavior")
            .isOpen(true)
            .reportedAt(LocalDateTime.of(2023, 1, 1, 10, 0))
            .build();

        pageable = PageRequest.of(0, 10);

    }

    @Test
    void create_withValidData_shouldSaveAndReturnDetailDto() {
        when(userService.getCurrentUser()).thenReturn(reporter);
        when(jobRequestRepository.findById(1L)).thenReturn(Optional.of(jobRequest));
        when(reportRepository.save(any(Report.class))).thenReturn(report);
        when(reportMapper.reportToDetailDto(report)).thenReturn(new ReportDetailDto());

        var result = reportService.create(createDto);

        assertNotNull(result);
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        Report savedReport = captor.getValue();

        assertEquals(reporter, savedReport.getReporter());
        assertEquals(target, savedReport.getTarget());
        assertEquals(createDto.getType(), savedReport.getType());
        assertEquals(createDto.getReason(), savedReport.getReason());
        assertTrue(savedReport.isOpen());
    }

    @Test
    void create_whenJobRequestHasNullCustomer_shouldSetTargetToNull() {
        jobRequest.setCustomer(null);
        when(userService.getCurrentUser()).thenReturn(reporter);
        when(jobRequestRepository.findById(1L)).thenReturn(Optional.of(jobRequest));
        when(reportRepository.save(any(Report.class))).thenReturn(report);

        reportService.create(createDto);

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        Report savedReport = captor.getValue();

        assertNull(savedReport.getTarget());
    }

    @Test
    void create_whenJobRequestNotFound_shouldThrowNotFoundException() {
        when(userService.getCurrentUser()).thenReturn(reporter);
        when(jobRequestRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reportService.create(createDto));
    }

    @Test
    void findAll_shouldReturnAllReportsOrEmptyList() {
        when(reportRepository.findAll()).thenReturn(List.of(report));
        when(reportMapper.reportToListDtoList(List.of(report))).thenReturn(List.of(new ReportListDto()));
        List<ReportListDto> resultWithReports = reportService.findAll();
        assertEquals(1, resultWithReports.size());

        when(reportRepository.findAll()).thenReturn(Collections.emptyList());
        when(reportMapper.reportToListDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());
        List<ReportListDto> resultWithoutReports = reportService.findAll();
        assertTrue(resultWithoutReports.isEmpty());
    }

    @Test
    void findAllByReporter_shouldReturnReportsForReporterOrEmptyList() {
        when(userService.getCurrentUser()).thenReturn(reporter);

        when(reportRepository.findAllByReporter(reporter)).thenReturn(List.of(report));
        when(reportMapper.reportToListDtoList(List.of(report))).thenReturn(List.of(new ReportListDto()));
        List<ReportListDto> resultWithReports = reportService.findAllByReporter();
        assertEquals(1, resultWithReports.size());

        when(reportRepository.findAllByReporter(reporter)).thenReturn(Collections.emptyList());
        when(reportMapper.reportToListDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());
        List<ReportListDto> resultWithoutReports = reportService.findAllByReporter();
        assertTrue(resultWithoutReports.isEmpty());
    }

    @Test
    void findAllByTarget_shouldReturnReportsForTargetOrEmptyList() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        when(reportRepository.findAllByTarget(target)).thenReturn(List.of(report));
        when(reportMapper.reportToListDtoList(List.of(report))).thenReturn(List.of(new ReportListDto()));
        List<ReportListDto> resultWithReports = reportService.findAllByTarget(2L);
        assertEquals(1, resultWithReports.size());

        when(reportRepository.findAllByTarget(target)).thenReturn(Collections.emptyList());
        when(reportMapper.reportToListDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());
        List<ReportListDto> resultWithoutReports = reportService.findAllByTarget(2L);
        assertTrue(resultWithoutReports.isEmpty());
    }

    @Test
    void findAllByTarget_whenUserNotFound_shouldThrowNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> reportService.findAllByTarget(999L));
    }

    @Test
    void closeReport_withOpenReport_shouldSetReportToClosed() {
        report.setOpen(true);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        reportService.closeReport(1L);

        assertFalse(report.isOpen());
        verify(reportRepository).save(report);
    }

    @Test
    void closeReport_whenReportNotFound_shouldThrowNotFoundException() {
        when(reportRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> reportService.closeReport(999L));
    }

    @Test
    void closeReport_whenAlreadyClosed_shouldThrowIllegalStateException() {
        report.setOpen(false);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        assertThrows(IllegalStateException.class, () -> reportService.closeReport(1L));
    }

    @Test
    void findById_withExistingId_shouldReturnDetailDto() {
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportMapper.reportToDetailDto(report)).thenReturn(new ReportDetailDto());
        assertNotNull(reportService.findById(1L));
        verify(reportMapper).reportToDetailDto(report);
    }

    @Test
    void findById_withNonExistingId_shouldThrowNotFoundException() {
        when(reportRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> reportService.findById(999L));
    }

    @Test
    void openReport_withClosedReport_shouldSetReportToOpen() {
        report.setOpen(false);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        reportService.openReport(1L);
        assertTrue(report.isOpen());
        verify(reportRepository).save(report);
    }

    @Test
    void openReport_whenReportNotFound_shouldThrowNotFoundException() {
        when(reportRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> reportService.openReport(999L));
    }

    @Test
    void openReport_whenAlreadyOpen_shouldThrowIllegalStateException() {
        report.setOpen(true);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        assertThrows(IllegalStateException.class, () -> reportService.openReport(1L));
    }
}