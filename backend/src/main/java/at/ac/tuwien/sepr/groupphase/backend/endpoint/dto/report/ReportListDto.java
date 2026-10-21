package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report;

import at.ac.tuwien.sepr.groupphase.backend.type.ReportType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportListDto {
    private Long id;
    private String reporterUsername;
    private String targetUsername;
    private String jobRequestTitle;
    private ReportType type;
    private String reason;
    @JsonProperty("isOpen")
    private boolean open;
    private LocalDateTime reportedAt;
}
