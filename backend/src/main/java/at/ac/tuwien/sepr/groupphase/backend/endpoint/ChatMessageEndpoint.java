package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatMessageActionDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatMessageActionNotification;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatMessageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatMessagePayloadDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatNotificationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.ChatMessageMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ChatMessage;
import at.ac.tuwien.sepr.groupphase.backend.service.ChatService;
import at.ac.tuwien.sepr.groupphase.backend.type.MessageAction;
import jakarta.validation.Valid;
import java.lang.invoke.MethodHandles;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatMessageEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ChatService chatService;
    private final SimpMessagingTemplate broker;
    private final ChatMessageMapper chatMessageMapper;

    @Autowired
    public ChatMessageEndpoint(ChatService chatService, SimpMessagingTemplate broker, ChatMessageMapper chatMessageMapper) {
        this.chatService = chatService;
        this.broker = broker;
        this.chatMessageMapper = chatMessageMapper;
    }

    @MessageMapping("/chat/{chat_id}/sendMessage")
    public void sendMessage(
        @DestinationVariable("chat_id") Long chatId,
        @Valid @Payload ChatMessageDto chatMessageDto,
        Principal principal
    ) {
        LOGGER.info("Received message send request for chat {} from user '{}': {}", chatId, principal.getName(), chatMessageDto);

        String username = principal.getName();

        ChatMessage chatMessage = chatService.saveMessage(username, chatId, chatMessageDto);
        ChatMessagePayloadDto payload = chatMessageMapper.entityToPayLoadDto(chatMessage);

        String destination = "/topic/chat/" + chatId;
        LOGGER.info("Payload to send: '{}'", payload.toString());

        broker.convertAndSend(destination, payload);

        LOGGER.debug("Broadcasted message to {}: {}", destination, payload);

        String recipient = chatService.getOtherChatParticipant(chatId, username);

        ChatNotificationDto notification = ChatNotificationDto.builder()
            .chatId(chatId)
            .username(username)
            .messageType(payload.getMessageType())
            .message(payload.getMessage())
            .build();

        broker.convertAndSendToUser(
            recipient,
            "/queue/notifications",
            notification
        );

        LOGGER.debug("Sent notification to {}: {}", recipient, notification);
    }


    @MessageMapping("/chat/{chat_id}/read")
    public void readMessage(
        @DestinationVariable("chat_id") Long chatId,
        Principal principal
    ) {
        LOGGER.info("Received message read for chat {} from user '{}'", chatId, principal.getName());

        String username = principal.getName();

        chatService.setMessagesToRead(username, chatId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("read", true);
        payload.put("reader", username);

        broker.convertAndSend(
            "/topic/chat/" + chatId + "/read",
            payload
        );

        LOGGER.debug("Sent read message to {}: {}", chatId, username);
    }

    @MessageMapping("/chat/{chat_id}/messageAction")
    public void messageAction(
        @DestinationVariable("chat_id") Long chatId,
        @Valid @Payload ChatMessageActionDto dto,
        Principal principal
    ) {
        String username = principal.getName();
        ChatMessageActionNotification payload;

        if (dto.getAction() == MessageAction.DELETE) {
            payload = chatService.deleteMessage(username, chatId, dto.getMessageId());
        } else { // EDIT
            payload = chatService.editMessage(username, chatId, dto.getMessageId(), dto.getNewMessage());
        }

        String dest = "/topic/chat/" + chatId + "/messageAction";
        broker.convertAndSend(dest, payload);
        LOGGER.debug("Sent delete/edit message to {}: {}", dest, payload);
    }

}
