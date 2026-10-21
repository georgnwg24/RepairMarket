package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PageDto<T> {
    private List<T> content;
    private int totalElements;
    private int pageSize;
    private int offset;

}

