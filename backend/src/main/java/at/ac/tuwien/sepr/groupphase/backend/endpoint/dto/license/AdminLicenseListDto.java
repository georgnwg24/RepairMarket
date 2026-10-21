package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license;

import at.ac.tuwien.sepr.groupphase.backend.type.LicenseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminLicenseListDto {

    private Long id;
    private String filename;
    private LicenseStatus status;
    private LocalDateTime uploadTime;
    private String description;
    private String username;
}
