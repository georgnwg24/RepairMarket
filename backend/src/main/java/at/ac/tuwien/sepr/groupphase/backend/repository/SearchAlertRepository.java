package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.SearchAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchAlertRepository extends JpaRepository<SearchAlert, Long> {

    List<SearchAlert> findByWorker(ApplicationUser worker);

    @Query(value = """
        SELECT sa.* FROM search_alert sa
        JOIN app_user w ON sa.worker_id = w.id
        JOIN job_request jr ON jr.id = :jobRequestId
        JOIN property p ON jr.property_id = p.id
        LEFT JOIN search_alert_categories sac ON sac.id = sa.id
        WHERE (
            NOT EXISTS (SELECT 1 FROM search_alert_categories sac2 WHERE sac2.id = sa.id)
            OR sac.category = jr.category
          )
          AND (
            sa.keywords IS NULL
            OR LOWER(jr.title) LIKE CONCAT('%', LOWER(sa.keywords), '%')
            OR LOWER(jr.description) LIKE CONCAT('%', LOWER(sa.keywords), '%')
          )
          AND (
            sa.max_distance IS NULL OR
                (
                    (p.latitude IS NOT NULL AND p.longitude IS NOT NULL AND
                    w.latitude IS NOT NULL AND w.longitude IS NOT NULL)
                    AND (6371 * acos(cos(radians(w.latitude)) * cos(radians(p.latitude)) * cos(radians(p.longitude) - radians(w.longitude)) + sin(radians(w.latitude)) * sin(radians(p.latitude)))) < sa.max_distance
                )
            )
        """, nativeQuery = true)
    List<SearchAlert> findMatchingAlertsForJobRequest(@Param("jobRequestId") Long jobRequestId);
}
