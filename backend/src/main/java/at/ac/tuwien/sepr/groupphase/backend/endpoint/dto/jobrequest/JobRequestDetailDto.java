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
 * Represents a Data Transfer Object (DTO) for detailed JobRequest Information.
 * This record encapsulates essential JobRequest attributes.
 */

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class JobRequestDetailDto {

    private Long id;
    private String title;
    private String description;
    private Category category;
    private JobStatus status;
    private LocalDate deadline;
    private Long propertyId;

}