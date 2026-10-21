package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.searchalert.SearchAlertCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.searchalert.SearchAlertDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.SearchAlert;
import at.ac.tuwien.sepr.groupphase.backend.type.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface SearchAlertMapper {

    @Mapping(source = "id", target = "id")
    SearchAlertDetailDto toDetailDto(SearchAlert searchAlert);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "worker", ignore = true)
    @Mapping(target = "categories", expression = "java(mapCategories(dto.getCategories()))")
    SearchAlert toEntity(SearchAlertCreateDto dto);

    default List<Category> mapCategories(List<String> categories) {
        if (categories == null) {
            return List.of();
        }
        return categories.stream()
            .map(String::toUpperCase)
            .map(Category::valueOf)
            .collect(Collectors.toList());
    }
}
