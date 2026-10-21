package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findByUser(ApplicationUser user);

    Optional<PushSubscription> findByEndpoint(String endpoint);

    void deleteByUser(ApplicationUser user);

    @Query("SELECT ps FROM PushSubscription ps WHERE ps.user.role = at.ac.tuwien.sepr.groupphase.backend.type.Role.ADMIN")
    List<PushSubscription> findAllByAdminRole();

}
