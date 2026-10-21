package at.ac.tuwien.sepr.groupphase.backend.endpoint;


import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page.PageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserUpdateDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserUpdateRestDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;
import java.util.List;

@RestController
@RequestMapping(path = UserEndpoint.BASE_PATH)
public class UserEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final String BASE_PATH = "/api/v1/users";
    private final UserService userService;

    @Autowired
    public UserEndpoint(UserService userService) {
        this.userService = userService;
    }

    @PermitAll
    @PutMapping(path = "/edit")
    public ResponseEntity<?> update(
        @Valid @RequestBody UserUpdateRestDto toUpdate,
        BindingResult bindingResult) {
        LOGGER.info("PUT " + BASE_PATH + "/{}", toUpdate);
        LOGGER.debug("Received JSON: {}", toUpdate);
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(bindingResult.getAllErrors());
        }
        String username = getCurrentUser().getUsername();
        LOGGER.info("PUT " + BASE_PATH);
        LOGGER.debug("Body of request:\n{}", toUpdate);
        return ResponseEntity.ok(userService.update(toUpdate.updateWithUsername(username)));
    }

    @PermitAll
    @GetMapping("/edit")
    public ResponseEntity<UserUpdateDetailDto> getUserForEdit() {
        String username = getCurrentUser().getUsername();
        LOGGER.info("GET " + BASE_PATH + "/{}", username);
        return ResponseEntity.ok(userService.getUserByUserNameForEdit(username));
    }

    @PermitAll
    @GetMapping(path = "{id}")
    public ResponseEntity<UserDetailDto> getUser(
        @PathVariable("id") Long id) {
        LOGGER.info("GET " + BASE_PATH + "/{}", id);
        return ResponseEntity.ok(userService.getUserDetailsById(id));
    }


    private ApplicationUser getCurrentUser() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userService.findUserByUsername(username).orElseThrow(() -> {
            LOGGER.error("Error while searching for user: {}", username);
            return new NotFoundException();
        });
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageDto<UserListDto> listAllUsers(
        @RequestParam(defaultValue = "0") int offset,
        @RequestParam(defaultValue = "10") int limit,
        @RequestParam(value = "username", required = false) String username

    ) {
        if (username == null || username.isBlank()) {
            return userService.getAllUsersByPage(offset, limit);
        }
        return userService.searchUsers(username, offset, limit);
    }

    @DeleteMapping(path = "/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
        ApplicationUser current = getCurrentUser();
        if (current.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userService.deleteUserByUsername(username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Ban the user with the given id.
     */
    @PatchMapping(path = "/{id}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> banUser(@PathVariable Long id) {
        userService.banUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Unban the user with the given id.
     */
    @PatchMapping(path = "/{id}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unbanUser(@PathVariable Long id) {
        userService.unbanUser(id);
        return ResponseEntity.noContent().build();
    }

}
