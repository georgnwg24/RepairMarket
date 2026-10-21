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
public class LicenseDetailDto {
    private Long id;
    private String filename;
    private String description;
    private LicenseStatus status;
    private LocalDateTime uploadTime;
    private String mediaType;
}
