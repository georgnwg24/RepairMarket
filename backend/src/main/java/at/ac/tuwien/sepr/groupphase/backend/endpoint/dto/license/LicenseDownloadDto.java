package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license;

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
public class LicenseDownloadDto {
    private String filename;
    private byte[] file;
    private String mediaType;
}
