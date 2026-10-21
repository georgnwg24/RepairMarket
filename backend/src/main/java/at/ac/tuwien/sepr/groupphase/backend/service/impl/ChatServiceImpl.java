package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatImageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatListItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatMessageActionNotification;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatMessageDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatMessageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.CreatedChatDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.ChatMapper;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.ChatMessageMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.Chat;
import at.ac.tuwien.sepr.groupphase.backend.entity.ChatMessage;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.exception.FileSizeLimitExceededException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ImageInvalidContentTypeException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ImageUploadException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ImageValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.ChatMessageRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ChatRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobOfferRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReportRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.ChatService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.type.JobOfferStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ChatMapper chatMapper;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Override
    @Transactional
    public CreatedChatDto findOrCreateChat(ApplicationUser user, long jobRequestId) {
        final JobRequest jobRequest = this.jobRequestRepository.findById(jobRequestId).orElseThrow();

        Optional<Chat> chat = this.chatRepository.findChatByJobRequest_Id(jobRequestId);

        ApplicationUser worker = (user.getRole() == Role.WORKER)
            ? user
            : this.jobOfferRepository.findJobOfferByJobRequestAndStatus(jobRequest, JobOfferStatus.ACCEPTED).orElseThrow().getWorker();

        CreatedChatDto chatDto;
        if (chat.isPresent()) { // return existing chatId
            chatDto = CreatedChatDto.builder()
                .chatId(chat.get().getId())
                .build();
        } else {
            Chat chatEntity = Chat.builder()
                .chatMessages(new LinkedList<>())
                .createdAt(LocalDateTime.now())
                .jobRequest(jobRequest)
                .customer(jobRequest.getCustomer())
                .worker(worker)
                .build();

            this.chatRepository.save(chatEntity);

            chatDto = CreatedChatDto.builder()
                .chatId(chatEntity.getId())
                .build();
        }

        return chatDto;
    }

    @Override
    @Transactional
    public void deleteChat(ApplicationUser applicationUser, long chatId) {
        LOGGER.trace("Deleting chat {}", chatId);
        Chat chat = this.chatRepository.findById(chatId).orElseThrow(
            () -> new EntityNotFoundException("Chat with id " + chatId + " does not exist"));

        if (!applicationUser.getId().equals(chat.getWorker().getId())
            && !applicationUser.getId().equals(chat.getCustomer().getId())) {
            throw new AccessDeniedException("You are not a participant of this chat");
        }

        chat.getChatMessages().forEach(chatMessage -> reportRepository.nullifyChatMessageReferences(chatMessage));

        this.chatRepository.deleteChat(chat, this.chatMessageRepository);
    }

    @Override
    @Transactional
    public List<ChatMessageDetailDto> getChatMessages(ApplicationUser applicationUser, long chatId) {
        LOGGER.trace("Getting chat messages for chat {}", chatId);
        Chat chat = this.chatRepository.findById(chatId).orElseThrow(
            () -> new EntityNotFoundException("Chat with id " + chatId + " does not exist"));

        if (!applicationUser.getId().equals(chat.getWorker().getId())
            && !applicationUser.getId().equals(chat.getCustomer().getId())) {
            throw new AccessDeniedException("You are not a participant of this chat");
        }

        List<ChatMessage> messages = new LinkedList<>(chat.getChatMessages());

        messages.stream()
            .filter(message -> !message.getSender().getId().equals(applicationUser.getId()) && !message.isRead())
            .forEach(message -> message.setRead(true));


        LOGGER.debug("Message list from chatId {} : {}", chatId, messages);

        return chat.getChatMessages().stream()
            .map(m -> {
                ChatMessageDetailDto dto = ChatMessageDetailDto.builder()
                    .id(m.getId())
                    .senderUsername(m.getSender().getUsername())
                    .messageType(m.getMessageType())
                    .message(m.getMessage())
                    .mediaName(m.getMediaName())
                    .mediaUrl(m.getMediaUrl())
                    .read(m.isRead())
                    .edited(m.isEdited())
                    .timestamp(m.getTimestamp())
                    .build();

                return dto;
            })
            .collect(Collectors.toList());
    }

    @Override
    public ChatMessage getLastMessage(ApplicationUser applicationUser, long chatId) {
        LOGGER.trace("Getting last message from counterpart in chat: {}", chatId);
        Chat chat = this.chatRepository.findById(chatId).orElseThrow(
            () -> new EntityNotFoundException("Chat with id " + chatId + " does not exist"));

        if (!applicationUser.getId().equals(chat.getWorker().getId())
            && !applicationUser.getId().equals(chat.getCustomer().getId())) {
            throw new AccessDeniedException("You are not a participant of this chat");
        }

        ChatMessage lastMessage = chat.getChatMessages()
            .stream()
            .filter(message -> !message.getSender().getId().equals(applicationUser.getId()))
            .findFirst()
            .orElse(null);

        if (chat.getWorker().getId().equals(applicationUser.getId())) {
            LOGGER.debug("Last Message from {} : {}", chat.getCustomer().getUsername(), lastMessage);
        } else {
            LOGGER.debug("Last Message from {} : {}", chat.getWorker().getUsername(), lastMessage);
        }

        return lastMessage;
    }

    @Override
    @Transactional
    public ChatMessage saveMessage(String username, long chatId, ChatMessageDto chatMessageDto) {
        LOGGER.trace("Saving message to chat {}: {}", chatId, chatMessageDto);

        if (!isMember(chatId, username)) {
            throw new AccessDeniedException("You are not a participant of this chat");
        }

        Chat chat = this.chatRepository.findById(chatId).orElseThrow(
            () -> new EntityNotFoundException("Chat with id " + chatId + " does not exist")
        );
        ApplicationUser user = userService.findUserByUsername(username).orElseThrow(
            () -> new UsernameNotFoundException(username)
        );


        ChatMessage message = chatMessageMapper.dtoToEntity(chatMessageDto);
        message.setSender(user);
        message.setChat(chat);
        message.setTimestamp(LocalDateTime.now());

        LOGGER.debug("Message saved: {}", message);

        return this.chatMessageRepository.save(message);
    }

    @Override
    @Transactional
    public boolean isMember(long chatId, String username) {
        Chat chat = this.chatRepository.findById(chatId).orElseThrow(
            () -> new EntityNotFoundException("Chat with id " + chatId + " does not exist")
        );
        return chat.getWorker().getUsername().equals(username) || chat.getCustomer().getUsername().equals(username);
    }

    @Override
    @Transactional
    public String getOtherChatParticipant(long chatId, String username) {
        Chat chat = this.chatRepository.findById(chatId).orElseThrow(
            () -> new EntityNotFoundException("Chat with id " + chatId + " does not exist")
        );
        return username.equals(chat.getWorker().getUsername()) ? chat.getCustomer().getUsername() : chat.getWorker().getUsername();
    }

    @Override
    @Transactional
    public List<ChatListItemDto> getAllChatsOfUser(String username) {
        LOGGER.debug("Getting all chats of user {}", username);

        ApplicationUser user = userService.findUserByUsername(username).orElseThrow();

        List<ChatListItemDto> chats;

        if (user.getRole().equals(Role.CUSTOMER)) {
            chats = user.getCustomerChats()
                .stream()
                .map(chat -> chatMapper.chatToChatListItemDto(chat, user.getId()))
                .collect(Collectors.toList());
        } else { // must be Worker bcs of @RolesAllowed
            chats = user.getWorkerChats()
                .stream()
                .map(chat -> chatMapper.chatToChatListItemDto(chat, user.getId()))
                .collect(Collectors.toList());
        }

        LOGGER.debug("All chats of user {} : {}", user, chats);
        return chats;
    }

    @Override
    @Transactional
    public void setMessagesToRead(String username, Long chatId) {
        if (!isMember(chatId, username)) {
            throw new AccessDeniedException("You are not a participant of this chat");
        }

        Chat chat = this.chatRepository.findById(chatId).orElseThrow(
            () -> new EntityNotFoundException("Chat with id " + chatId + " does not exist")
        );
        ApplicationUser user = userService.findUserByUsername(username).orElseThrow(
            () -> new UsernameNotFoundException(username)
        );

        chat.getChatMessages().stream()
            .filter(message -> !message.getSender().getId().equals(user.getId()))
            .forEach(message -> message.setRead(true));
    }

    @Override
    @Transactional
    public ChatMessageActionNotification deleteMessage(String username, Long chatId, Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
            .filter(msg -> msg.getChat().getId().equals(chatId))
            .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        if (!message.getSender().getUsername().equals(username)) {
            throw new AccessDeniedException("This is not your message");
        }

        reportRepository.nullifyChatMessageReferences(message);
        chatMessageRepository.deleteById(messageId);

        return ChatMessageActionNotification.builder()
            .messageId(messageId)
            .newMessage("")
            .edited(false)
            .deleted(true)
            .build();
    }

    @Override
    @Transactional
    public ChatMessageActionNotification editMessage(String username, Long chatId, Long messageId, String newMessage) {
        ChatMessage message = chatMessageRepository.findById(messageId)
            .filter(msg -> msg.getChat().getId().equals(chatId))
            .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        if (!message.getSender().getUsername().equals(username)) {
            throw new AccessDeniedException("This is not your message");
        }

        message.setMessage(newMessage);
        message.setEdited(true);

        return ChatMessageActionNotification.builder()
            .messageId(messageId)
            .newMessage(newMessage)
            .edited(true)
            .deleted(false)
            .build();
    }

    @Value("${chat.upload.max-file-size}")
    private DataSize maxFileSize;

    @Value("${chat.upload.base-path}")
    private String uploadBasePath;

    @Value("${chat.upload.base-url}")
    private String uploadBaseUrl;

    @Override
    public ChatImageDto uploadImage(MultipartFile image) {
        validateImage(image);

        String original = image.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.')).toLowerCase();
        }
        // UUID to make it "unique"
        String filename = UUID.randomUUID() + "_" + image.getOriginalFilename().substring(0, image.getOriginalFilename().length() - ext.length()) + ext;

        Path dir = Paths.get(uploadBasePath);
        Path target = dir.resolve(filename);

        try {
            Files.createDirectories(dir);
            image.transferTo(target);
        } catch (IOException e) {
            throw new ImageUploadException("Failed to write image: " + e.getMessage());
        }

        String mediaUrl = uploadBaseUrl + "/" + filename;

        return ChatImageDto.builder()
            .mediaUrl(mediaUrl)
            .mediaName(original)
            .build();
    }

    private void validateImage(MultipartFile image) {
        if (image.isEmpty()) {
            throw new ImageValidationException("No image was given.");
        }
        if (image.getSize() > maxFileSize.toBytes()) {
            throw new FileSizeLimitExceededException("File is too large, maximum: " + bytesToMb(maxFileSize.toBytes()) + "MB, actual: " + bytesToMb(image.getSize()) + "MB");
        }
        if (image.getContentType() == null || !(image.getContentType().contains("image/") || image.getContentType().contains("application/pdf"))) {
            throw new ImageInvalidContentTypeException("Invalid file type: " + image.getContentType());
        }
    }

    private double bytesToMb(long bytes) {
        double mega = bytes / 1024.0 / 1024.0;
        return Math.round(mega * 100.0) / 100.0;
    }
}
