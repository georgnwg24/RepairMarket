package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PropertyDetailDto {
    private Long id;
    private String countryCode;
    private String postalCode;
    private String area;
    private String address;
    private Float longitude;
    private Float latitude;
}
