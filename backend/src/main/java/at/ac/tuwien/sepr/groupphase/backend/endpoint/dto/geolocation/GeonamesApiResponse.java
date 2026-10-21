package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.geolocation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeonamesApiResponse {
    @JsonProperty("total_count")
    private int totalCount;
    private List<GeonamesResult> results;
}
