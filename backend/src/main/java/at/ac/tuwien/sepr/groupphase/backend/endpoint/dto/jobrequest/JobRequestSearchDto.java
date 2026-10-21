package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest;

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
public class JobRequestSearchDto {
    private String title;
    private String description;
    private String category;
    private String status;
    private LocalDate deadline;
    private Long propertyId;
    private Integer distance;
    private Float lowestPriceMin;
    private Float lowestPriceMax;
}