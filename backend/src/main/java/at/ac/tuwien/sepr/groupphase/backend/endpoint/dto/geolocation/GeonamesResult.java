package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.geolocation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeonamesResult {
    @JsonProperty("place_name")
    private String placeName;
    private Float latitude;
    private Float longitude;
}