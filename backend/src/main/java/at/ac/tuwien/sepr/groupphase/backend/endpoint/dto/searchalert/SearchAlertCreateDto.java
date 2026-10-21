package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.searchalert;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchAlertCreateDto {

    private String keywords;

    @PositiveOrZero
    private Integer maxDistance;

    private List<String> categories;
}
