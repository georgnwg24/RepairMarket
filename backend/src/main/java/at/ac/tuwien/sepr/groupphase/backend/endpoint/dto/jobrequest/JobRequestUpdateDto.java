package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest;

import at.ac.tuwien.sepr.groupphase.backend.type.Category;
import at.ac.tuwien.sepr.groupphase.backend.type.JobStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * Represents a Data Transfer Object (DTO) for updating JobRequests.
 * This record encapsulates all necessary fields for updating a JobRequest.
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class JobRequestUpdateDto {

    private Long id;

    @NotBlank(message = "Title must not be blank")
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;

    @Size(max = 4095, message = "Description must be at most 4095 characters")
    private String description;

    @NotNull(message = "Category must not be null")
    private Category category;

    @NotNull(message = "Status must not be null")
    private JobStatus status;

    @Future(message = "Deadline must be a future date")
    private LocalDate deadline;

    private Long propertyId;
    /*
    private Long customerId;
     */

}
