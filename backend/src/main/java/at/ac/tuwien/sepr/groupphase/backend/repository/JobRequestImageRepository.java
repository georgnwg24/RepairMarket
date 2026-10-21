package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequestImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRequestImageRepository extends JpaRepository<JobRequestImage, Long> {

    /**
     * Retrieves all JobRequestImage entities associated with the given job request ID,
     * ordered by their display position in ascending order.
     *
     * @param jobRequestId the unique identifier of the job request
     * @return a sorted list of {@link JobRequestImage} entities for the specified job request
     */
    List<JobRequestImage> findByJobRequestIdOrderByDisplayPositionAsc(Long jobRequestId);

    /**
     * Retrieves a specific JobRequestImage entity by its job request ID and image ID.
     *
     * @param jobRequestId the unique identifier of the job request
     * @param imageId      the unique identifier of the image
     * @return an Optional containing the matching {@link JobRequestImage} if found, or empty otherwise
     */
    Optional<JobRequestImage> findByJobRequestIdAndId(Long jobRequestId, Long imageId);

    /**
     * Counts the number of images associated with a specific job request.
     *
     * @param jobRequestId the unique identifier of the job request
     * @return the total count of images for the specified job request
     */
    int countByJobRequestId(Long jobRequestId);

    /**
     * Checks if an image exists for a specific job request.
     *
     * @param jobRequestId ID of the job request
     * @param imageId ID of the image to check
     * @return true if the image exists and belongs to the specified job request
     */
    boolean existsByJobRequestIdAndId(Long jobRequestId, Long imageId);

    /**
     * Deletes all Job Requests Images with an ID less than the specified value.
     *
     * @param l the threshold ID; all Job Requests Images with an ID less than this value will be deleted
     */
    void deleteAllByIdLessThan(long l);
}
