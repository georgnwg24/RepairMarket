package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.License;
import at.ac.tuwien.sepr.groupphase.backend.type.LicenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LicenseRepository extends JpaRepository<License, Long> {

    /**
     * Finds all Properties associated with a specific customer ID.
     *
     * @param workerId the ID of the customer whose properties should be retrieved
     * @return a list of all Properties belonging to the specified customer
     */
    List<License> findAllByWorkerId(Long workerId);

    /**
     * Deletes all Licenses with an ID less than the specified value.
     *
     * @param l the threshold ID; all Licenses with an ID less than this value will be deleted
     */
    void deleteAllByIdLessThan(long l);

    /**
     * Fetch all licenses whose status is PENDING.
     */
    List<License> findAllByStatus(LicenseStatus status);

    /**
     * Fetch all licenses by their Status.
     */
    Page<License> findAllByStatus(LicenseStatus status, Pageable pageable);

    /**
     * Fetch all licenses by their Status and Username.
     */
    Page<License> findAllByStatusAndWorker_UsernameContainingIgnoreCase(LicenseStatus status, String username, Pageable pageable);


    /**
     * Returns true if the given worker ID has at least one license
     * in the given status.
     */
    boolean existsByWorkerIdAndStatus(Long workerId, LicenseStatus status);
}
