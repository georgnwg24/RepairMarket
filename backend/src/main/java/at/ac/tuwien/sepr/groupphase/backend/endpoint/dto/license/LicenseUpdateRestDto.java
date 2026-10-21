package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.license;

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
public class LicenseUpdateRestDto {
    @Size(max = 255, message = "filename must be at most 255 characters")
    private String filename;
    @Size(max = 255, message = "Description must be at most 4095 characters")
    private String description;
    private String mediaType;

    public LicenseUpdateDto toUpdateWithId(Long id) {
        return new LicenseUpdateDto(id, this.filename, this.description, this.mediaType);
    }

}
