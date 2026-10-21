package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    /**
     * Finds all Properties associated with a specific customer ID.
     *
     * @param customerId the ID of the customer whose properties should be retrieved
     * @return a list of all Properties belonging to the specified customer
     */
    List<Property> findAllByCustomerId(Long customerId);

    /**
     * Deletes all Properties with an ID less than the specified value.
     *
     * @param l the threshold ID; all Properties with an ID less than this value will be deleted
     */
    void deleteAllByIdLessThan(long l);

}
