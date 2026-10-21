package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer;

import at.ac.tuwien.sepr.groupphase.backend.type.JobOfferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Represents a Data Transfer Object (DTO) for detailed JobOffer Information.
 * This record encapsulates essential JobOffer attributes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobOfferDetailDto {
    private Long id;
    private String comment;
    private Instant createdAt;
    private JobOfferStatus status;
    private Float price;
    private Long jobRequestId;
    private Long workerId;
}
