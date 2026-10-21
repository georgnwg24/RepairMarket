package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class UserUpdateRestDto {

    @NotBlank(message = "E-Mail must not be blank!")
    @Email(message = "E-Mail must be valid!")
    private String email;
    @Size(max = 255, message = "Firstname must be at most 255 characters")
    private String firstName;
    @Size(max = 255, message = "Lastname must be at most 255 characters")
    private String lastName;
    @Size(max = 3, message = "Country code must be at most 3 characters")
    private String countryCode;
    @NotBlank(message = "Zip Code must not be blank")
    @Size(max = 10, message = "Zip Code must be at most 10 characters")
    private String postalCode;
    @NotBlank(message = "Area must not be blank")
    @Size(max = 255, message = "Area must be at most 255 characters")
    private String area;

    public UserUpdateDto updateWithUsername(String username) {
        return new UserUpdateDto(username, email, firstName, lastName, countryCode, postalCode, area);
    }

}
