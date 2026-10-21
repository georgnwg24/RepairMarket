package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page.PageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report.ReportCreateDto;
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
import at.ac.tuwien.sepr.groupphase.backend.service.ReportService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.type.ReportType;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ReportMapper reportMapper;
    private final UserService userService;
    private final JobRequestRepository jobRequestRepository;
    private final ChatService chatService;
    private final ChatMessageRepository chatMessageRepository;

    @Autowired
    public ReportServiceImpl(ReportRepository reportRepository, UserRepository userRepository, ReportMapper reportMapper, UserService userService,
                             JobRequestRepository jobRequestRepository, ChatService chatService, ChatMessageRepository chatMessageRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.reportMapper = reportMapper;
        this.userService = userService;
        this.jobRequestRepository = jobRequestRepository;
        this.chatService = chatService;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    @Override
    public ReportDetailDto create(ReportCreateDto createDto) {
        LOGGER.trace("create() with parameters: {}", createDto);

        ApplicationUser reporter = userService.getCurrentUser();
        JobRequest jr = jobRequestRepository
            .findById(createDto.getJobRequestId())
            .orElseThrow(() -> new NotFoundException("JobRequest not found: " + createDto.getJobRequestId()));
        ApplicationUser target = jr.getCustomer();

        Report report = Report.builder()
            .reporter(reporter)
            .target(target)
            .type(createDto.getType())
            .reason(createDto.getReason())
            .jobRequest(jr)
            .build();

        Report saved = reportRepository.save(report);
        return reportMapper.reportToDetailDto(saved);
    }

    @Override
    @Transactional
    public ReportDetailDto create(ReportMessageDto messageDto) {
        LOGGER.trace("create() with parameters: {}", messageDto);
        ApplicationUser reporter = userService.getCurrentUser();
        String targetUsername = chatService.getOtherChatParticipant(messageDto.getChatId(), reporter.getUsername());
        ApplicationUser target = userService.findUserByUsername(targetUsername)
            .orElseThrow(() -> new NotFoundException("User not found: " + targetUsername));

        ChatMessage chatMessage =
            chatMessageRepository.findChatMessageByIdAndChat_Id(messageDto.getMessageId(), messageDto.getChatId())
                    .orElseThrow(() -> new NotFoundException("ChatMessage not found for id=" + messageDto.getMessageId() + " in chat=" + messageDto.getChatId()));

        reportRepository.findByReporterAndChatMessage(reporter, chatMessage)
            .ifPresent(report -> {
                throw new AlreadyReportedException("You have already reported this chat message.");
            });

        Report report = Report.builder()
            .reporter(reporter)
            .target(target)
            .chatMessage(chatMessage)
            .type(ReportType.MESSAGE)
            .reason(messageDto.getReason())
            .build();

        Report saved = reportRepository.save(report);
        return reportMapper.reportToDetailDto(saved);
    }

    @Override
    public List<ReportListDto> findAll() {
        return reportMapper.reportToListDtoList(reportRepository.findAll());
    }

    @Override
    public PageDto<ReportListDto> findAllByStatus(int offset, int limit, boolean status, String username) {
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Order.asc("reportedAt"), Sort.Order.asc("id")));
        Page<Report> reportPage;

        if (username == null || username.isBlank()) {
            reportPage = reportRepository.findAllByIsOpen(status, pageable);
        } else {
            reportPage = reportRepository.findAllByIsOpenAndUsernameContainingIgnoreCase(status, username.trim(), pageable);
        }

        List<ReportListDto> reportListDto = reportMapper.reportToListDtoList(reportPage.getContent());
        return new PageDto<>(reportListDto, (int) reportPage.getTotalElements(), limit, offset);
    }



    @Override
    public List<ReportListDto> findAllByReporter() {
        ApplicationUser reporter = userService.getCurrentUser();
        return reportMapper.reportToListDtoList(reportRepository.findAllByReporter(reporter));
    }

    @Override
    public List<ReportListDto> findAllByTarget(Long targetId) {
        ApplicationUser target = userRepository.findById(targetId)
            .orElseThrow(() -> new NotFoundException("Target user not found"));
        return reportMapper.reportToListDtoList(reportRepository.findAllByTarget(target));
    }

    @Override
    public ReportDetailDto findById(Long reportId) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new NotFoundException("Report not found: " + reportId));
        return reportMapper.reportToDetailDto(report);
    }

    @Override
    public void closeReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new NotFoundException("Report not found"));
        if (!report.isOpen()) {
            throw new IllegalStateException("Report is already closed");
        }
        report.setOpen(false);
        reportRepository.save(report);
    }

    @Override
    public void openReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new NotFoundException("Report not found"));
        if (report.isOpen()) {
            throw new IllegalStateException("Report is already opened");
        }
        report.setOpen(true);
        reportRepository.save(report);
    }

}
