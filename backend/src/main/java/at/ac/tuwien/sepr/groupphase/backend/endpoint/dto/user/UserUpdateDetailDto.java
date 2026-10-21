package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user;

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
public class UserUpdateDetailDto {

    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String countryCode;
    private String postalCode;
    private String area;
}
