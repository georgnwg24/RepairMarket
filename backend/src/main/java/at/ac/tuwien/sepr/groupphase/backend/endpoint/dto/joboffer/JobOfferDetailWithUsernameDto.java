package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer;

import at.ac.tuwien.sepr.groupphase.backend.type.JobOfferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobOfferDetailWithUsernameDto {
    private Long id;
    private String comment;
    private LocalDateTime createdAt;
    private JobOfferStatus status;
    private Float price;
    private Long jobRequestId;
    private Long workerId;
    private String workerUsername;
    private Float workerAverageRating;
}
