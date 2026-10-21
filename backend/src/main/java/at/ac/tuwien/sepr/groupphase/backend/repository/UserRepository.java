package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<ApplicationUser, Long> {

    /**
     * Finds a user by their email address.
     *
     * @param email the email address to search for
     * @return the user if found, or empty otherwise
     */
    Optional<ApplicationUser> findUserByEmail(String email);

    /**
     * Checks whether a user exists with the specified email address.
     *
     * @param email the email address to check
     * @return true if a user exists with the email, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return the user or empty otherwise
     */
    Optional<ApplicationUser> findUserByUsername(String username);

    /**
     * Checks whether a user exists with the specified username.
     *
     * @param username the username to check
     * @return true if a user exists with the username, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Deletes all users with an ID less than the specified value.
     *
     * @param l the threshold ID; all users with an ID less than this value will be deleted
     */
    void deleteAllByIdLessThan(long l);

    Page<ApplicationUser> findByUsernameContainingIgnoreCaseAndRoleNotAndUsernameNot(String usernamePart, Role excludedRole, String excludedUsername, Pageable pageable);

    Page<ApplicationUser> findByRoleNotAndUsernameNot(Role excludedRole, String excludedUsername, Pageable pageable);


}
