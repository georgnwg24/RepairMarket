package at.ac.tuwien.sepr.groupphase.backend.repository;


import at.ac.tuwien.sepr.groupphase.backend.entity.JobOffer;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.type.JobOfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {

    Optional<JobOffer> findJobOfferByJobRequestAndStatus(JobRequest jobRequest, JobOfferStatus status);

    void deleteAllByIdLessThan(long l);

    @Query("""
        select jo from JobOffer jo
        join fetch jo.jobRequest jr
        join fetch jr.receivedJobOffers
        where jo.worker.id = :workerId
         and jo.worker.banned = false
         and jr.customer.banned = false
        order by jo.createdAt desc
        """)
    List<JobOffer> findAllByWorkerIdWithJobRequestAndOffers(@Param("workerId") Long workerId);

    @Query("""
        select jo from JobOffer jo
        join fetch jo.jobRequest jr
        where jo.worker.id = :workerId
         and jo.worker.banned = false
         and jr.customer.banned = false
         and jo.status <> :status
        order by jo.createdAt desc
        """)
    Page<JobOffer> findAllByWorkerIdAndStatusNotWithJobRequest(
        @Param("workerId") Long workerId,
        @Param("status") JobOfferStatus status,
        Pageable pageable
    );


    boolean existsByJobRequestIdAndWorkerId(Long requestId, Long workerId);

    boolean existsByJobRequestIdAndWorkerIdAndStatus(Long requestId, Long workerId, JobOfferStatus status);

    @Query("""
        select jo from JobOffer jo
        join fetch jo.worker w
        join fetch jo.jobRequest jr
        where jr.customer.id = :customerId
         and jr.customer.banned = false
         and jo.worker.banned = false
        order by jo.createdAt desc
        """)
    List<JobOffer> findAllForCustomerWithWorker(@Param("customerId") Long customerId);

    JobOffer findByJobRequestIdAndStatus(Long jobRequestId, JobOfferStatus status);

    @Query("""
        select jo from JobOffer jo
        join fetch jo.worker
        join fetch jo.jobRequest jr
        where jo.id = :offerId
         and jo.worker.banned = false
         and jr.customer.banned = false
        """)
    JobOffer findByIdWithWorkerAndRequest(@Param("offerId") Long offerId);

    @Query("""
        select jo from JobOffer jo
        join fetch jo.worker
        where jo.jobRequest.id = :jobRequestId
          and jo.status in (at.ac.tuwien.sepr.groupphase.backend.type.JobOfferStatus.DONE,
                       at.ac.tuwien.sepr.groupphase.backend.type.JobOfferStatus.HIDDEN)
          and jo.worker.banned = false
          and jo.jobRequest.customer.banned = false
        """)
    Optional<JobOffer> findCompletedOfferByJobRequestId(@Param("jobRequestId") Long jobRequestId);
}
