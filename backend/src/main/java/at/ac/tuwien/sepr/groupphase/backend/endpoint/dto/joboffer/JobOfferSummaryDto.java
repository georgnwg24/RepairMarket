package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer;

import at.ac.tuwien.sepr.groupphase.backend.type.JobOfferStatus;
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
public class JobOfferSummaryDto {
    private Long id;
    private Float price;
    private JobOfferStatus status;
    private Long jobRequestId;
    private String requestTitle;
    private float lowestPrice;
}
