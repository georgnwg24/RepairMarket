package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.type.Category;
import at.ac.tuwien.sepr.groupphase.backend.type.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRequestRepository extends JpaRepository<JobRequest, Long> {

    /**
     * Finds all Job Requests associated with a specific customer ID who is not banned.
     * The associated Property is eagerly fetched to prevent LazyInitializationException.
     *
     * @param customerId the ID of the customer whose job requests should be retrieved
     * @return a list of all Job Requests belonging to the specified customer
     */
    @Query("SELECT j FROM JobRequest j LEFT JOIN FETCH j.property WHERE j.customer.id = :customerId AND j.customer.banned = false")
    List<JobRequest> findAllByCustomerIdAndCustomerBannedFalse(@Param("customerId") Long customerId);

    /**
     * Finds all Job Requests by status from customers who are not banned.
     * The associated Property is eagerly fetched to prevent LazyInitializationException.
     *
     * @param status the status of the job requests to find
     * @return a list of Job Requests matching the status
     */
    @Query("SELECT j FROM JobRequest j LEFT JOIN FETCH j.property WHERE j.status = :status AND j.customer.banned = false")
    List<JobRequest> findAllByStatusAndCustomerBannedFalse(@Param("status") JobStatus status);


    /**
     * Checks whether any Job Request exists for the given property ID.
     *
     * @param propertyId the ID of the property to check
     * @return {@code true} if a Job Request exists for the given property ID, {@code false} otherwise
     */
    boolean existsByPropertyId(Long propertyId);

    /**
     * Deletes all Job Requests with an ID less than the specified value.
     *
     * @param l the threshold ID; all Job Requests with an ID less than this value will be deleted
     */
    void deleteAllByIdLessThan(long l);

    @Query("""
        select distinct jr from JobRequest jr
        left join fetch jr.property
        left join fetch jr.receivedJobOffers ro
        left join fetch ro.worker
        where jr.status = :status and jr.customer.banned = false
        order by jr.createdAt
        """)
    List<JobRequest> findAllByStatusWithOffers(@Param("status") JobStatus status);

    @Query("""
        select jr from JobRequest jr
        join fetch jr.customer
        left join fetch jr.property
        where jr.id = :id and jr.customer.banned = false
        """)
    Optional<JobRequest> findByIdWithCustomer(@Param("id") Long id);

    /**
     * Search for job requests by the given criteria.
     * Parameters can be null, in which case they are ignored in the search.
     */
    @Query("SELECT j FROM JobRequest j LEFT JOIN FETCH j.property WHERE "
        + "j.customer.id = :customerId AND "
        + "j.customer.banned = false AND "
        + "j.status <> 'HIDDEN' AND "
        + "(:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND "
        + "(:categories IS NULL OR j.category IN :categories) AND "
        + "(:statuses IS NULL OR j.status IN :statuses) AND "
        + "(:deadline IS NULL OR j.deadline <= :deadline) AND "
        + "(:propertyId IS NULL OR j.property.id = :propertyId)")
    Page<JobRequest> searchJobRequestsCustomer(
        @Param("customerId") Long customerId,
        @Param("title") String title,
        @Param("categories") List<Category> categories,
        @Param("statuses") List<JobStatus> statuses,
        @Param("deadline") LocalDate deadline,
        @Param("propertyId") Long propertyId,
        Pageable pageable
    );


    /**
     * Finds all PENDING job requests from not banned users matching criteria within a specific distance.
     * Uses a native query for the required math functions.
     */
    @Query(value = """
        SELECT j.* FROM job_request j
        JOIN property p ON j.property_id = p.id
        JOIN app_user c ON j.customer_id = c.id
        LEFT JOIN (SELECT job_request_id, MIN(price) as lowest_price FROM job_offer WHERE status = 'PENDING' GROUP BY job_request_id) AS offers ON j.id = offers.job_request_id
        WHERE j.status = 'PENDING' AND c.banned = false
        AND (:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%')))
        AND (:categories IS NULL OR j.category IN (:categories))
        AND (:deadline IS NULL OR j.deadline >= :deadline)
        AND ((:lowestPriceMin IS NULL OR offers.lowest_price >= :lowestPriceMin) AND (:lowestPriceMax IS NULL OR offers.lowest_price <= :lowestPriceMax))
        AND (
                :distance IS NULL OR
                (
                    (p.latitude IS NOT NULL AND p.longitude IS NOT NULL)
                    AND (6371 * acos(cos(radians(:latitude)) * cos(radians(p.latitude)) * cos(radians(p.longitude) - radians(:longitude)) + sin(radians(:latitude)) * sin(radians(p.latitude)))) < :distance
                )
            )
        AND NOT EXISTS (
            SELECT 1
            FROM job_offer o
            WHERE o.job_request_id = j.id
              AND o.worker_id = :workerId
              AND o.status = 'PENDING'
        )
        """, nativeQuery = true)
    Page<JobRequest> searchOpenJobRequestsWorkerWithDistance(
        @Param("title") String title,
        @Param("categories") List<String> categories,
        @Param("deadline") LocalDate deadline,
        @Param("latitude") Float latitude,
        @Param("longitude") Float longitude,
        @Param("distance") Integer distance,
        @Param("lowestPriceMin") Float lowestPriceMin,
        @Param("lowestPriceMax") Float lowestPriceMax,
        @Param("workerId") Long workerId,
        Pageable pageable
    );

    @Query("SELECT j FROM JobRequest j LEFT JOIN FETCH j.property WHERE "
        + "(:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND "
        + "j.status <> 'HIDDEN' AND "
        + "(:categories IS NULL OR j.category IN :categories) AND "
        + "(:statuses IS NULL OR j.status IN :statuses) AND "
        + "(:deadline IS NULL OR j.deadline <= :deadline)")
    Page<JobRequest> searchJobRequestsAdmin(
        @Param("title") String title,
        @Param("categories") List<Category> categories,
        @Param("statuses") List<JobStatus> statuses,
        @Param("deadline") LocalDate deadline,
        Pageable pageable
    );
}