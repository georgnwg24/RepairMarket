package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AreaLookupDto {
    private List<String> areaNames;
}