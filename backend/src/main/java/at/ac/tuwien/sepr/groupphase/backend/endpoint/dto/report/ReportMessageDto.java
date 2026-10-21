package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report;


import jakarta.validation.constraints.NotNull;
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
public class ReportMessageDto {
    @NotNull(message = "messageId must not be null")
    private Long messageId;

    @NotNull(message = "chatId must not be null")
    private Long chatId;

    private String reason;
}
