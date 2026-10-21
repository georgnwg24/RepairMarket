package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat;

import at.ac.tuwien.sepr.groupphase.backend.type.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatNotificationDto {
    private Long chatId;
    private String username;
    private MessageType messageType;
    private String message;
}
