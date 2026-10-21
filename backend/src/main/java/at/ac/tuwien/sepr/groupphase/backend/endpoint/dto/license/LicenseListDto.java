package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license;

import at.ac.tuwien.sepr.groupphase.backend.type.LicenseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseListDto {

    private Long id;
    private String filename;
    private LicenseStatus status;
    private LocalDateTime uploadTime;
}
