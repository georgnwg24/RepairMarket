package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license;

import at.ac.tuwien.sepr.groupphase.backend.type.LicenseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LicenseStatusUpdateDto {
    private Long id;
    private LicenseStatus status;
}
