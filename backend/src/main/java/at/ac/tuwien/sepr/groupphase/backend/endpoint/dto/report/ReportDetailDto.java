package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.report;

import at.ac.tuwien.sepr.groupphase.backend.type.Category;
import at.ac.tuwien.sepr.groupphase.backend.type.JobStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.ReportType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDetailDto {
    // Report info
    private Long id;
    private String reporterUsername;
    private String targetUsername;
    private ReportType type;
    private String reason;
    @JsonProperty("isOpen")
    private boolean open;
    private LocalDateTime reportedAt;

    // JobRequest info
    private Long jobId;
    private String jobTitle;
    private String description;
    private Category category;
    private JobStatus status;
    private LocalDate deadline;

    // Message info
    private Long messageId;
    private String message;
    private String mediaName;
    private String mediaUrl;
    private LocalDateTime timestamp;
}
