package at.ac.tuwien.sepr.groupphase.backend.service;


import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page.PageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegistrationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserUpdateDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface UserService extends UserDetailsService {

    /**
     * Find a user in the context of Spring Security based on the username.
     * <br>
     * For more information have a look at this tutorial:
     * https://www.baeldung.com/spring-security-authentication-with-a-database
     *
     * @param username the username
     * @return a Spring Security user
     * @throws UsernameNotFoundException is thrown if the specified user does not exist
     */
    @Override
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

    /**
     * Find an application user based on the username.
     *
     * @param username the username
     * @return an application user
     */
    Optional<ApplicationUser> findUserByUsername(String username);


    /**
     * Register a new user.
     */
    ApplicationUser register(UserRegistrationDto userRegistrationDto);

    /**
     * Log in a user.
     *
     * @param userLoginDto login credentials
     * @return the JWT, if successful
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are bad
     */
    String login(UserLoginDto userLoginDto);

    /**
     * Delete an account from a given user.
     *
     * @param username the username
     */
    void deleteUserByUsername(String username);

    /**
     * Updates user details.
     *
     * @param userUpdate the data transfer object containing updated user information
     * @return the updated user data transfer object
     */
    UserUpdateDto update(UserUpdateDto userUpdate);

    /**
     * Retrieves detailed user information for editing purposes.
     *
     * @param username the username of the user
     * @return detailed user information for update
     */
    UserUpdateDetailDto getUserByUserNameForEdit(String username);

    /**
     * Retrieves detailed user information for editing purposes.
     *
     * @param id the id of the user
     * @return detailed user information
     */
    UserDetailDto getUserDetailsById(Long id);

    ApplicationUser getCurrentUser();


    /**
     * Retrieve all users (for admin view) by Page.
     */
    PageDto<UserListDto> getAllUsersByPage(int offset, int limit);

    /**
     * Ban the user with the given id.
     *
     * @param id the user’s id
     */
    void banUser(Long id);

    /**
     * Un‐ban the user with the given id.
     *
     * @param id the user’s id
     */
    void unbanUser(Long id);

    PageDto<UserListDto> searchUsers(String usernamePart, int offset, int limit);
}
