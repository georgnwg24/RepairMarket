package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat;

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
public class ChatDto {
    private Long id;

    private long jobRequestId;
    private String jobRequestTitle;

    private String counterPartName;

    private String lastMessageOfCounterpart; // can be null
    private LocalDateTime lastMessageOfCounterpartTime; // can be null
    private int numberOfUnreadMessages;
}
