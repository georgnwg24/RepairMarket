package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequestimage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobRequestImageDataDto {

    private byte[] imageData;

    private String contentType;

}
