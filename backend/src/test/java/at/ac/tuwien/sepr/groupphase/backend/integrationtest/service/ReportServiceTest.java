package at.ac.tuwien.sepr.groupphase.backend.integrationtest.service;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page.PageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportMessageDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.*;
import at.ac.tuwien.sepr.groupphase.backend.repository.*;
import at.ac.tuwien.sepr.groupphase.backend.service.ReportService;
import at.ac.tuwien.sepr.groupphase.backend.type.MessageType;
import at.ac.tuwien.sepr.groupphase.backend.type.ReportType;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRepository chatRepository;

    private ApplicationUser reporter;
    private ApplicationUser target;
    private JobRequest jobRequest;

    @BeforeEach
    void setUp() {
        reporter = userRepository.save(ApplicationUser.builder()
            .username("reporter")
            .email("reporter@example.com")
            .role(Role.CUSTOMER)
            .password("password")
            .build());

        target = userRepository.save(ApplicationUser.builder()
            .username("target")
            .email("target@example.com")
            .role(Role.WORKER)
            .password("password")
            .build());

        jobRequest = jobRequestRepository.save(JobRequest.builder()
            .customer(target)
            .title("Leaky Faucet Repair")
            .build());

        simulateCurrentUser(reporter);
    }

    @Test
    void create_reportForJobRequest_shouldPersist() {
        ReportCreateDto dto = new ReportCreateDto();
        dto.setJobRequestId(jobRequest.getId());
        dto.setType(ReportType.JOB_REQUEST);
        dto.setReason("This job request is suspicious");

        ReportDetailDto result = reportService.create(dto);

        assertThat(result).isNotNull();
        assertThat(result.getReporterUsername()).isEqualTo(reporter.getUsername());
        assertThat(result.getTargetUsername()).isEqualTo(target.getUsername());
        assertThat(result.getType()).isEqualTo(ReportType.JOB_REQUEST);
        assertThat(result.getJobId()).isEqualTo(jobRequest.getId());

        assertThat(result.getMessageId()).isNull();
        assertThat(result.getMessage()).isNull();
    }

    @Test
    void create_reportForChatMessage_shouldWork() {
        Chat chat = chatRepository.save(Chat.builder()
            .customer(reporter)
            .worker(target)
            .jobRequest(jobRequest)
            .build());

        ChatMessage msg = chatMessageRepository.save(ChatMessage.builder()
            .chat(chat)
            .sender(target)
            .message("Offensive message")
            .messageType(MessageType.TEXT)
            .build());

        ReportMessageDto dto = new ReportMessageDto();
        dto.setChatId(chat.getId());
        dto.setMessageId(msg.getId());
        dto.setReason("Offensive");

        ReportDetailDto result = reportService.create(dto);

        assertThat(result).isNotNull();
        assertThat(result.getReporterUsername()).isEqualTo(reporter.getUsername());
        assertThat(result.getTargetUsername()).isEqualTo(target.getUsername());
        assertThat(result.getType()).isEqualTo(ReportType.MESSAGE);
        assertThat(result.getMessageId()).isEqualTo(msg.getId());
        assertThat(result.getMessage()).isEqualTo("Offensive message");

        assertThat(result.getJobId()).isNull();
        assertThat(result.getJobTitle()).isNull();
    }

    @Test
    void findAllByReporter_shouldReturnUserReports() {
        ReportCreateDto dto = new ReportCreateDto();
        dto.setJobRequestId(jobRequest.getId());
        dto.setType(ReportType.JOB_REQUEST);
        dto.setReason("Reason 1");
        reportService.create(dto);

        List<ReportListDto> reports = reportService.findAllByReporter();
        assertThat(reports).hasSize(1);
        assertThat(reports.getFirst().getReporterUsername()).isEqualTo(reporter.getUsername());
    }

    @Test
    void findAllByTarget_shouldReturnReportsAgainstTarget() {
        ReportCreateDto dto = new ReportCreateDto();
        dto.setJobRequestId(jobRequest.getId());
        dto.setType(ReportType.JOB_REQUEST);
        dto.setReason("Reason 2");
        reportService.create(dto);

        List<ReportListDto> reports = reportService.findAllByTarget(target.getId());
        assertThat(reports).hasSize(1);
        assertThat(reports.getFirst().getTargetUsername()).isEqualTo(target.getUsername());
    }

    @Test
    void closeAndOpenReport_shouldToggleStatus() {
        ReportCreateDto dto = new ReportCreateDto();
        dto.setJobRequestId(jobRequest.getId());
        dto.setType(ReportType.JOB_REQUEST);
        dto.setReason("Reason 3");
        ReportDetailDto created = reportService.create(dto);

        reportService.closeReport(created.getId());
        Report closed = reportRepository.findById(created.getId()).orElseThrow();
        assertThat(closed.isOpen()).isFalse();

        reportService.openReport(created.getId());
        Report reopened = reportRepository.findById(created.getId()).orElseThrow();
        assertThat(reopened.isOpen()).isTrue();
    }

    @Test
    void findAllByStatus_withFilter_shouldReturnPagedResults() {
        for (int i = 0; i < 5; i++) {
            reportService.create(new ReportCreateDto(jobRequest.getId(), ReportType.JOB_REQUEST, "Reason " + i));
        }

        PageDto<?> page = reportService.findAllByStatus(0, 3, true, "target");

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isGreaterThan(3);
    }

    @Test
    void closeReport_withNonExistingId_shouldThrowException() {
        long nonExistingId = 99999L;
        assertThatThrownBy(() -> reportService.closeReport(nonExistingId))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void create_reportForChatMessage_withInvalidMessageId_shouldThrowException() {
        Chat chat = chatRepository.save(Chat.builder()
            .customer(reporter)
            .worker(target)
            .jobRequest(jobRequest)
            .build());

        ReportMessageDto dto = new ReportMessageDto();
        dto.setChatId(chat.getId());
        dto.setMessageId(99999L);  // invalid/non-existent messageId
        dto.setReason("Invalid message");

        assertThatThrownBy(() -> reportService.create(dto))
            .isInstanceOf(RuntimeException.class);
    }

    private void simulateCurrentUser(ApplicationUser user) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        var auth = new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
