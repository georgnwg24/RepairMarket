package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat;

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
public class ChatMessageActionNotification {
    private Long messageId;
    private String newMessage;
    private boolean deleted;
    private boolean edited;
}
