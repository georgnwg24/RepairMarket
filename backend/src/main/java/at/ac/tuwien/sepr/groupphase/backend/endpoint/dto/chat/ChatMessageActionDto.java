package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat;

import at.ac.tuwien.sepr.groupphase.backend.type.MessageAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class ChatMessageActionDto {
    @NotNull
    private Long messageId;

    @NotNull
    private MessageAction action;

    @Size(max = 4095, message = "message max 4095 chars")
    private String newMessage; // edit
}
