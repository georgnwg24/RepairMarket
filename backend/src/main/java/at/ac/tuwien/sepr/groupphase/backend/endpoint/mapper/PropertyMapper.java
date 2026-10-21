package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyEditDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyListDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Property;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PropertyMapper {

    /**
     * Maps a Property entity to a ListDto.
     *
     * @param property the JobRequest that gets mapped
     * @return a jobRequestToUpdateDto with information from jobRequest
     */
    @Named("property")
    PropertyListDto propertyToListDto(Property property);

    @IterableMapping(qualifiedByName = "property")
    List<PropertyListDto> propertyToListDto(List<Property> property);

    Property createDtoToProperty(PropertyCreateDto propertyCreateDto);

    Property updateDtoToProperty(PropertyEditDto propertyEditDto);

    PropertyDetailDto propertyToDetailDto(Property property);

}
