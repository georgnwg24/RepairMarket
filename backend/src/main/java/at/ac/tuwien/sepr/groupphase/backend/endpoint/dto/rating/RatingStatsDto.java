package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.rating;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingStatsDto {
    private float average;
    private long count;
}
