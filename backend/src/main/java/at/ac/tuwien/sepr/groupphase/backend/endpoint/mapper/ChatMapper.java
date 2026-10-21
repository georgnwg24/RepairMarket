package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatListItemDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Chat;
import at.ac.tuwien.sepr.groupphase.backend.entity.ChatMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class ChatMapper {

    abstract ChatDto toDto(Chat chat);

    public ChatListItemDto chatToChatListItemDto(Chat chat, long currentUserId) {

        List<ChatMessage> chatMessages = chat.getChatMessages();

        Optional<ChatMessage> lastCounterpart = chatMessages.stream()
            .filter(msg -> !msg.getSender().getId().equals(currentUserId))
            .findFirst();

        String latestMessage = lastCounterpart.map(ChatMessage::getMessage).orElse(null);
        LocalDateTime latestMessageTime = lastCounterpart.map(ChatMessage::getTimestamp).orElse(null);

        int unreadMessages = (int) chatMessages.stream()
                .filter(chatMessage -> !chatMessage.getSender().getId().equals(currentUserId))
                .filter(chatMessage -> !chatMessage.isRead())
                .count();

        return ChatListItemDto.builder()
            .id(chat.getId())
            .jobRequestId(chat.getJobRequest().getId())
            .jobRequestTitle(chat.getJobRequest().getTitle())
            .counterPartName((currentUserId == chat.getWorker().getId()) ? chat.getCustomer().getUsername() : chat.getWorker().getUsername())
            .counterPartBanned((currentUserId == chat.getWorker().getId()) ? chat.getCustomer().isBanned() : chat.getWorker().isBanned())
            .lastMessageOfCounterpart(latestMessage)
            .lastMessageOfCounterpartTime(latestMessageTime)
            .numberOfUnreadMessages(unreadMessages)
            .build();
    }
}
