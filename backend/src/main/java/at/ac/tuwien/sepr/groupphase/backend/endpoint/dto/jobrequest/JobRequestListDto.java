package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest;

import at.ac.tuwien.sepr.groupphase.backend.type.Category;
import at.ac.tuwien.sepr.groupphase.backend.type.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * Represents a Data Transfer Object (DTO) for JobRequest that get Listed.
 * This record encapsulates essential JobRequest attributes for Lists.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequestListDto {
    private Long id;
    private String title;
    private String description;
    private Category category;
    private JobStatus status;
    private LocalDate deadline;
    private String address;
}
