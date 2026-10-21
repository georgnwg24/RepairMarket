package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatImageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatListItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatMessageActionNotification;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatMessageDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.ChatMessageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat.CreatedChatDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.ChatMessage;
import java.util.List;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.web.multipart.MultipartFile;

public interface ChatService {

    CreatedChatDto findOrCreateChat(ApplicationUser user, long jobRequestId);

    void deleteChat(ApplicationUser applicationUser, long chatId);

    List<ChatMessageDetailDto> getChatMessages(ApplicationUser applicationUser, long chatId);

    ChatMessage getLastMessage(ApplicationUser applicationUser, long chatId);

    ChatMessage saveMessage(String username, long chatId, ChatMessageDto chatMessageDto);

    boolean isMember(long chatId, String username);

    String getOtherChatParticipant(long chatId, String username);

    List<ChatListItemDto> getAllChatsOfUser(String username);

    ChatImageDto uploadImage(MultipartFile image) throws FileUploadException;

    void setMessagesToRead(String username, Long chatId);

    ChatMessageActionNotification deleteMessage(String username, Long chatId, Long messageId);

    ChatMessageActionNotification editMessage(String username, Long chatId, Long messageId, String newMessage);
}
