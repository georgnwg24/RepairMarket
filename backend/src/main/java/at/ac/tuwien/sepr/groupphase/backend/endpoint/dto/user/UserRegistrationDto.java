package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user;

import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import at.ac.tuwien.sepr.groupphase.backend.validation.PasswordMatches;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@EqualsAndHashCode
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@PasswordMatches
@Getter
@Setter

public class UserRegistrationDto {

    @NotBlank(message = "Username must not be blank!")
    private String username;

    @NotBlank(message = "Password must not be blank!")
    @Size(min = 8, message = "Password must be at least 8 characters long!")
    @ToString.Exclude
    private String password;

    @NotBlank(message = "Email must not be blank!")
    @Email(message = "Email must be valid!")
    private String email;

    @NotNull(message = "Role must be not null!")
    private Role role;

    private String firstName;

    private String lastName;

    @Size(max = 3, message = "Country code must be at most 3 characters")
    private String countryCode;

    @Size(max = 10, message = "Zip Code must be at most 10 characters")
    private String postalCode;

    @Size(max = 255, message = "Area must be at most 255 characters")
    private String area;

    @NotBlank(message = "Confirm your password!")
    @ToString.Exclude
    private String confirmPassword;

    @AssertTrue(message = "Country, Postal Code, and Area are required for workers.")
    @JsonIgnore
    public boolean isWorkerAddressValid() {
        if (role != Role.WORKER) {
            return true;
        }

        boolean countryValid = countryCode != null && !countryCode.trim().isEmpty();
        boolean postalCodeValid = postalCode != null && !postalCode.trim().isEmpty();
        boolean areaValid = area != null && !area.trim().isEmpty();

        return countryValid && postalCodeValid && areaValid;
    }
}
