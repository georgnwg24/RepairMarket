package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report;

import at.ac.tuwien.sepr.groupphase.backend.type.ReportType;
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
public class ReportCreateDto {
    @NotNull(message = "Job request ID must not be null")
    private Long jobRequestId;

    @NotNull(message = "Report type must not be null")
    private ReportType type;

    @NotBlank(message = "Reason must not be blank")
    @Size(max = 1023, message = "Reason must be at most 1023 characters")
    private String reason;
}
