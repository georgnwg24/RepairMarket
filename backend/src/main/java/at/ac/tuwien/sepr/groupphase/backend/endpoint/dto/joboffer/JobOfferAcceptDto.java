package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobOfferAcceptDto {
    private Long offerId;
    private LocalDateTime createdAt;
}
