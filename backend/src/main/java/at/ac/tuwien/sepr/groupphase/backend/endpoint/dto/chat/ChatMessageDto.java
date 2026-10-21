package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.chat;


import at.ac.tuwien.sepr.groupphase.backend.type.MessageType;
import jakarta.validation.constraints.NotBlank;
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
public class ChatMessageDto {

    @NotNull
    private MessageType messageType;

    @NotBlank(message = "Message cannot be empty")
    @Size(min = 1, max = 4095, message = "Message has to be between 1 and 255 length")
    private String message;

    @Size(max = 255, message = "Filename max 255 chars")
    private String mediaName;

    @Size(max = 1024, message = "URL too long")
    private String mediaUrl;

}
