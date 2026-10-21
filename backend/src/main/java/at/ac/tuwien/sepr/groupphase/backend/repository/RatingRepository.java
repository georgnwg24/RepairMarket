package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    @Query("""
        SELECT r FROM Rating r
        JOIN FETCH r.jobRequest
        JOIN FETCH r.toUser
        WHERE r.fromUser.id = :fromUserId AND r.jobRequest.id = :jobRequestId
        """)
    Optional<Rating> findByFromUserIdAndJobRequestId(@Param("fromUserId") Long fromUserId, @Param("jobRequestId") Long jobRequestId);

    List<Rating> findTop10ByToUserIdOrderByCreatedAtDesc(Long userId);

    List<Rating> findAllByToUserId(Long userId);

    /**
     * Deletes all Ratings with an ID less than the specified value.
     *
     * @param l the threshold ID; all Ratings with an ID less than this value will be deleted
     */
    void deleteAllByIdLessThan(long l);

    List<Rating> findAllByFromUser(ApplicationUser user);

    List<Rating> findAllByToUser(ApplicationUser user);

}
