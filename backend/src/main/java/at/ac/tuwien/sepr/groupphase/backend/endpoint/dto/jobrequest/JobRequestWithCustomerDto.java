package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest;

import at.ac.tuwien.sepr.groupphase.backend.type.Category;
import at.ac.tuwien.sepr.groupphase.backend.type.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequestWithCustomerDto {

    private Long id;
    private String title;
    private String description;
    private Category category;
    private JobStatus status;
    private LocalDate deadline;
    private Long propertyId;
    private Long customerId;
    private String customerUsername;

}
