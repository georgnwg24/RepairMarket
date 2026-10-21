package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class PropertyCreateDto {
    @NotBlank(message = "Country must not be blank")
    @Size(max = 3, message = "Country code must be at most 3 characters")
    private String countryCode;

    @NotBlank(message = "Zip Code must not be blank")
    @Size(max = 10, message = "Zip Code must be at most 10 characters")
    private String postalCode;

    @NotBlank(message = "Area must not be blank")
    @Size(max = 255, message = "Area must be at most 255 characters")
    private String area;

    @NotBlank(message = "Address must not be blank")
    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;
}
