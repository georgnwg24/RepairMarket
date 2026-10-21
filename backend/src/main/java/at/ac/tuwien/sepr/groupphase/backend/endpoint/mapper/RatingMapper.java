package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.rating.RatingDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Rating;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RatingMapper {

    RatingDto ratingToDto(Rating rating);

    Rating dtoToRating(RatingDto ratingDto);
}
