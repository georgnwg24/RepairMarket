package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat;

import at.ac.tuwien.sepr.groupphase.backend.type.MessageType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ChatMessagePayloadDto {
    private Long id;
    private MessageType messageType;
    private String message;
    private String mediaName;
    private String mediaUrl;
    private String senderUsername;
    private LocalDateTime timestamp;
}