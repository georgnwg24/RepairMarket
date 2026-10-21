package at.ac.tuwien.sepr.groupphase.backend.unittests.service;

import at.ac.tuwien.sepr.groupphase.backend.entity.Chat;
import at.ac.tuwien.sepr.groupphase.backend.entity.ChatMessage;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobOffer;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.CreatedChatDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatMessageDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatMessageActionNotification;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatMessageDto;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.ChatServiceImpl;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.ChatMapper;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.ChatMessageMapper;
import at.ac.tuwien.sepr.groupphase.backend.repository.ChatRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ChatMessageRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobOfferRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReportRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.type.JobOfferStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock ChatRepository chatRepository;
    @Mock ChatMapper chatMapper;
    @Mock ChatMessageRepository chatMessageRepository;
    @Mock ChatMessageMapper chatMessageMapper;
    @Mock UserService userService;
    @Mock JobRequestRepository jobRequestRepository;
    @Mock JobOfferRepository jobOfferRepository;
    @Mock ReportRepository reportRepository;

    @InjectMocks
    ChatServiceImpl chatService;

    ApplicationUser customer;
    ApplicationUser worker;
    JobRequest jobRequest;
    Chat existingChat;

    @BeforeEach
    void setUp() {
        customer = ApplicationUser.builder().id(10L).role(Role.CUSTOMER).username("cust").build();
        worker   = ApplicationUser.builder().id(20L).role(Role.WORKER).username("work").build();
        jobRequest = JobRequest.builder().id(5L).customer(customer).build();
        existingChat = Chat.builder()
            .id(100L)
            .jobRequest(jobRequest)
            .customer(customer)
            .worker(worker)
            .chatMessages(new LinkedList<>())
            .build();
    }

    @Test
    void findOrCreateChat_newChat_createsAndReturnsId() {
        when(jobRequestRepository.findById(5L)).thenReturn(Optional.of(jobRequest));
        when(chatRepository.findChatByJobRequest_Id(5L)).thenReturn(Optional.empty());
        when(jobOfferRepository.findJobOfferByJobRequestAndStatus(jobRequest, JobOfferStatus.ACCEPTED))
            .thenReturn(Optional.of(JobOffer.builder().worker(worker).build()));

        when(chatRepository.save(any(Chat.class))).thenAnswer(inv -> {
            Chat c = inv.getArgument(0);
            c.setId(200L);
            return c;
        });

        CreatedChatDto dto = chatService.findOrCreateChat(customer, 5L);

        assertThat(dto.getChatId()).isEqualTo(200L);
        verify(chatRepository).save(any(Chat.class));
    }


    @Test
    void findOrCreateChat_existingChat_returnsExistingId() {
        when(jobRequestRepository.findById(5L)).thenReturn(Optional.of(jobRequest));
        when(chatRepository.findChatByJobRequest_Id(5L)).thenReturn(Optional.of(existingChat));
        when(jobOfferRepository.findJobOfferByJobRequestAndStatus(jobRequest, JobOfferStatus.ACCEPTED))
            .thenReturn(Optional.of(JobOffer.builder().id(300L).worker(worker).build()));

        CreatedChatDto dto = chatService.findOrCreateChat(customer, 5L);

        assertThat(dto.getChatId()).isEqualTo(100L);
        verify(chatRepository, never()).save(any());
    }

    @Test
    void deleteChat_notParticipant_throwsAccessDenied() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(existingChat));
        ApplicationUser other = ApplicationUser.builder().id(99L).role(Role.CUSTOMER).build();

        assertThrows(AccessDeniedException.class,
            () -> chatService.deleteChat(other, 100L)
        );
    }

    @Test
    void getChatMessages_noChat_throwsEntityNotFound() {
        when(chatRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
            () -> chatService.getChatMessages(customer, 999L)
        );
    }

    @Test
    void isMember_returnsTrueForParticipant() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(existingChat));
        assertThat(chatService.isMember(100L, customer.getUsername())).isTrue();
    }

    @Test
    void getOtherChatParticipant_returnsCorrectName() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(existingChat));
        String other = chatService.getOtherChatParticipant(100L, customer.getUsername());
        assertThat(other).isEqualTo(worker.getUsername());
    }

    @Test
    void deleteMessage_valid_returnsNotification() {
        ChatMessage m = ChatMessage.builder().id(50L).chat(existingChat).sender(customer).build();
        when(chatMessageRepository.findById(50L)).thenReturn(Optional.of(m));
        doNothing().when(reportRepository).nullifyChatMessageReferences(m);

        ChatMessageActionNotification notif = chatService.deleteMessage(customer.getUsername(), 100L, 50L);
        assertThat(notif.getMessageId()).isEqualTo(50L);
        assertThat(notif.isDeleted()).isTrue();
    }

    @Test
    void editMessage_valid_returnsNotification() {
        ChatMessage m = ChatMessage.builder().id(60L).chat(existingChat).sender(customer).build();
        when(chatMessageRepository.findById(60L)).thenReturn(Optional.of(m));

        ChatMessageActionNotification notif = chatService.editMessage(customer.getUsername(), 100L, 60L, "new");

        assertThat(notif.isEdited()).isTrue();
        assertThat(m.getMessage()).isEqualTo("new");
    }
}
