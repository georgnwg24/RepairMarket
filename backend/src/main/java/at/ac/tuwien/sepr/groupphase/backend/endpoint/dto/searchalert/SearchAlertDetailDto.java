package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.searchalert;

import at.ac.tuwien.sepr.groupphase.backend.type.Category;
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
public class SearchAlertDetailDto {
    private Long id;

    private String keywords;

    private int maxDistance;

    private List<Category> categories;

    private boolean active;

    private int count;
}
