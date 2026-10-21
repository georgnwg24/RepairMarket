package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a Data Transfer Object (DTO) for properties that get Listed.
 * This record encapsulates essential property attributes for Lists.
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PropertyListDto {
    private Long id;
    private String countryCode;
    private String postalCode;
    private String area;
    private String address;
}
