package at.ac.tuwien.sepr.groupphase.backend.integrationtest.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegistrationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.exception.EmailAlreadyExistsException;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.UserAlreadyExistsException;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import jakarta.transaction.Transactional;

import java.util.Collections;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for {@link UserService}.
 */
@ActiveProfiles({"test", "datagen"})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class UserServiceTest {

    private final UserService userService;
    private final UserRepository userRepository;

    @Autowired
    public UserServiceTest(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Test
    void loadUserByUsername_whenUserExists_thenReturnUserDetails() {
        UserDetails userDetails = userService.loadUserByUsername("vince");
        assertEquals("vince", userDetails.getUsername());
    }

    @Test
    void loadUserByUsername_whenUserDoesNotExist_shouldThrowUsernameNotFoundException() {
        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("not exists"));
    }

    @Test
    @Transactional
    void register_whenValidNewUser_shouldCreateAndReturnUser() {
        String username = "TestingPenguin";
        UserRegistrationDto registrationDto = new UserRegistrationDto(
            username,
            "test1234!pass",
            "test.email@gmail.com",
            Role.CUSTOMER,
            "testFirstName",
            "testLastName",
            "AT",
            "1040",
            "Wieden",
            "test1234!pass"
        );
        ApplicationUser user = userService.register(registrationDto);
        assertEquals(username, user.getUsername());

        Optional<ApplicationUser> foundUser = userRepository.findUserByUsername(username);
        assertTrue(foundUser.isPresent());
        assertEquals(user.getId(), foundUser.get().getId());
    }

    @Test
    void register_whenUserAlreadyExists_shouldThrowsUsernameAlreadyExistsException() {
        UserRegistrationDto registrationDtoAlreadyExists = new UserRegistrationDto(
            "vince",
            "test1234!pass",
            "test.asemail@gmail.com",
            Role.CUSTOMER,
            "testFirstName",
            "testLastName",
            "AT",
            "1040",
            "Wieden",
            "test1234!pass"
        );
        assertThrows(UserAlreadyExistsException.class, () -> userService.register(registrationDtoAlreadyExists));
    }

    @Test
    void register_whenEmailAlreadyExists_shouldThrowEmailAlreadyExistsException() {
        UserRegistrationDto registrationDtoAlreadyExists = new UserRegistrationDto(
            "testtesttest",
            "test1234!pass",
            "vince@example.com",
            Role.WORKER,
            "testFirstName",
            "testLastName",
            "AT",
            "1040",
            "Wieden",
            "test1234!pass"
        );
        assertThrows(EmailAlreadyExistsException.class, () -> userService.register(registrationDtoAlreadyExists));
    }

    @Test
    @Transactional
    void delete_whenUserExists_shouldRemoveUser() {
        String username = "ToBeDeleted";
        UserRegistrationDto dto = new UserRegistrationDto(
            username,
            "securePass1!",
            "delete.me@example.com",
            Role.CUSTOMER,
            "First",
            "Last",
            "AT",
            "1040",
            "Wieden",
            "securePass1!"
        );
        userService.register(dto);

        Optional<ApplicationUser> before = userRepository.findUserByUsername(username);
        assertTrue(before.isPresent(), "precondition failed: user must exist before delete");

        userService.deleteUserByUsername(username);

        Optional<ApplicationUser> after = userRepository.findUserByUsername(username);
        assertFalse(after.isPresent(), "user should have been deleted");
    }

    @Test
    void delete_whenUserDoesNotExist_shouldThrowNotFoundException() {
        assertThrows(
            NotFoundException.class,
            () -> userService.deleteUserByUsername("no_such_user"),
            "Expected deleteUserByUsername to throw NotFoundException for unknown username"
        );
    }

    @Test
    @Transactional
    void getByUsername_whenCustomer_thenReturnCustomerDto() {
        ApplicationUser user = userRepository.findUserByUsername("vince")
            .orElseThrow(() -> new RuntimeException("User not found"));

        var result = userService.getUserByUserNameForEdit(user.getUsername());

        assertNotNull(result);
        assertEquals("vince", user.getUsername());
    }

    @Test
    @Transactional
    void updateUser_shouldUpdateUserData() {
        String testUsername = "vince";
        ApplicationUser existingUser = userRepository.findUserByUsername(testUsername)
            .orElseThrow(() -> new RuntimeException("User not found"));

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                testUsername,
                "12345678",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
            );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setUsername(existingUser.getUsername());
        updateDto.setFirstName("UpdatedFirstName");
        updateDto.setLastName("UpdatedLastName");
        updateDto.setEmail("test.customer.updated@example.com");
        updateDto.setArea("UpdatedArea");
        updateDto.setCountryCode("AT");
        updateDto.setPostalCode("1010");

        var updatedDtoResult = userService.update(updateDto);

        assertNotNull(updatedDtoResult);
        assertEquals("UpdatedFirstName", updatedDtoResult.getFirstName());
        assertEquals("UpdatedLastName", updatedDtoResult.getLastName());
        assertEquals("UpdatedArea", updatedDtoResult.getArea());

        ApplicationUser updatedUserInDb = userRepository.findUserByUsername(testUsername)
            .orElseThrow(() -> new RuntimeException("Updated user not found in DB"));
        assertEquals("UpdatedFirstName", updatedUserInDb.getFirstName());
        assertEquals("UpdatedLastName", updatedUserInDb.getLastName());
        assertEquals("UpdatedArea", updatedUserInDb.getArea());
        assertEquals("test.customer.updated@example.com", updatedUserInDb.getEmail());
    }
}