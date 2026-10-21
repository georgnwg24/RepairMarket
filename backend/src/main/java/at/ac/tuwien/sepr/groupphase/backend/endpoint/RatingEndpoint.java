package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.rating.RatingDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.rating.RatingStatsDto;
import at.ac.tuwien.sepr.groupphase.backend.service.RatingService;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import java.lang.invoke.MethodHandles;
import java.util.List;

@RestController
@RequestMapping(path = RatingEndpoint.BASE_PATH)
public class RatingEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final String BASE_PATH = "/api/v1/ratings";
    private final RatingService ratingService;

    @Autowired
    public RatingEndpoint(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PermitAll
    @GetMapping("/{jobRequestId}")
    public ResponseEntity<RatingDto> getRatingByRequestId(@PathVariable Long jobRequestId) {
        LOGGER.info("GET " + BASE_PATH + "/" + jobRequestId);

        try {
            RatingDto rating = ratingService.getRatingByRequestId(jobRequestId);

            if (rating == null) {
                return ResponseEntity.noContent().build(); // 204 for create-edit check
            }
            return ResponseEntity.ok(rating);
        } catch (Exception e) {
            LOGGER.error("Error fetching rating from request {}: {}", jobRequestId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rating not found", e);
        }
    }

    @PermitAll
    @PostMapping(path = "/{jobRequestId}", consumes = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Void> createRating(
        @PathVariable Long jobRequestId,
        @RequestBody RatingDto rating) {

        LOGGER.info("POST " + BASE_PATH + "/" + jobRequestId);

        try {
            ratingService.createRating(jobRequestId, rating);
        } catch (Exception e) {
            LOGGER.error("Error creating rating: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
        return ResponseEntity.ok().build();
    }

    @PermitAll
    @PutMapping(path = "/{jobRequestId}", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Void> updateRating(
        @PathVariable Long jobRequestId,
        @RequestBody RatingDto rating) {
        LOGGER.info("PUT " + BASE_PATH + "/" + jobRequestId);

        try {
            ratingService.updateRating(jobRequestId, rating);
        } catch (Exception e) {
            LOGGER.error("Error updating rating of jobrequest {}: {}", jobRequestId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
        return ResponseEntity.ok().build();
    }

    @PermitAll
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RatingDto>> getLatestRatings(@PathVariable Long userId) {
        LOGGER.info("GET " + BASE_PATH + "/user/{}", userId);

        try {
            List<RatingDto> ratings = ratingService.getLatestRatings(userId);
            return ResponseEntity.ok(ratings);
        } catch (Exception e) {
            LOGGER.error("Error fetching ratings for user {}: {}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not fetch ratings", e);
        }
    }

    @PermitAll
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<RatingStatsDto> getRatingStats(@PathVariable Long userId) {
        LOGGER.info("GET " + BASE_PATH + "/user/{}/stats", userId);

        try {
            RatingStatsDto stats = ratingService.getRatingStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            LOGGER.error("Error fetching rating stats for user {}: {}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not fetch rating stats", e);
        }
    }
}
