package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat;

import at.ac.tuwien.sepr.groupphase.backend.type.MessageType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDetailDto {
    private Long id;
    private String senderUsername;
    private MessageType messageType;
    private String message;
    private String mediaName;
    private String mediaUrl;
    private boolean read;
    private boolean edited;
    private LocalDateTime timestamp;
}
