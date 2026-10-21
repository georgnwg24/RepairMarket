package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.rating.RatingDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.rating.RatingStatsDto;

import java.nio.file.AccessDeniedException;
import java.util.List;

public interface RatingService {

    /**
     * Returns the rating from the logged-in user for the given jobRequestId if exists.
     * Otherwise, return null (the frontend uses this method to find out if the rating component should be loaded in create or edit mode).
     *
     * @param jobRequestId the jobRequestId for which the Rating should be searched.
     * @return             the stored RatingDto or null
     */
    RatingDto getRatingByRequestId(Long jobRequestId);

    /**
     * Create a new rating for the given job request from the logged-in user.
     *
     * @param jobRequestId the ID of the JobRequest to rate
     * @param ratingDto    the DTO containing stars and comment
     */
    void createRating(Long jobRequestId, RatingDto ratingDto) throws AccessDeniedException;

    /**
     * Update the rating for the given job request from the logged-in user.
     *
     * @param jobRequestId the ID of the JobOffer to update the rating from
     * @param ratingDto    the DTO containing stars and comment
     */
    void updateRating(Long jobRequestId, RatingDto ratingDto);

    /**
     * Returns up to the latest 10 received ratings of the given user.
     *
     * @param userId the userId for which the Rating should be fetched.
     * @return a list of RatingDtos
     */
    List<RatingDto> getLatestRatings(Long userId);

    /**
     * Returns the average stars of and amount of received ratings of the given user.
     *
     * @param userId the userId for which the Rating stats should be fetched.
     * @return a RatingStatsDto
     */
    RatingStatsDto getRatingStats(Long userId);
}
