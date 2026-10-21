package at.ac.tuwien.sepr.groupphase.backend.unittests.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.page.PageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegistrationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserUpdateDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.UserMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.exception.EmailAlreadyExistsException;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.UserAlreadyExistsException;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PropertyRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PushSubscriptionRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.RatingRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReportRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.UserServiceImpl;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String EMAIL = "test@example.com";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenizer jwtTokenizer;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PushSubscriptionRepository pushSubscriptionRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private JobRequestRepository jobRequestRepository;
    @Mock
    private RatingRepository ratingRepository;
    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private ApplicationUser user;
    private ApplicationUser adminUser;
    private ApplicationUser anotherUser;
    private UserRegistrationDto registrationDto;
    private UserLoginDto loginDto;
    private UserUpdateDto updateDto;

    @BeforeEach
    void setUp() {
        user = ApplicationUser.builder()
            .id(1L)
            .username(USERNAME)
            .password(ENCODED_PASSWORD)
            .email(EMAIL)
            .role(Role.CUSTOMER)
            .firstName("John")
            .lastName("Doe")
            .area("Vienna")
            .build();

        adminUser = ApplicationUser.builder()
            .id(99L)
            .username("adminuser")
            .password(ENCODED_PASSWORD)
            .email("admin@example.com")
            .role(Role.ADMIN)
            .firstName("Admin")
            .lastName("User")
            .area("AdminArea")
            .build();

        anotherUser = ApplicationUser.builder()
            .id(2L)
            .username("anotheruser")
            .password(ENCODED_PASSWORD)
            .email("another@example.com")
            .role(Role.CUSTOMER)
            .firstName("Jane")
            .lastName("Smith")
            .area("Graz")
            .build();


        registrationDto = UserRegistrationDto.builder()
            .username(USERNAME)
            .password(PASSWORD)
            .confirmPassword(PASSWORD)
            .email(EMAIL)
            .role(Role.CUSTOMER)
            .firstName("John")
            .lastName("Doe")
            .area("Vienna")
            .build();

        loginDto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .build();

        updateDto = UserUpdateDto.builder()
            .username(USERNAME) // Default for update, can be changed in tests
            .email("new@example.com")
            .firstName("Johnny")
            .lastName("D")
            .area("Graz")
            .build();
    }

    @Test
    void register_withValidData_shouldSucceed() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userMapper.userRegistrationDtoToApplicationUser(registrationDto)).thenReturn(user);
        when(userRepository.save(any(ApplicationUser.class))).thenReturn(user);

        ApplicationUser result = userService.register(registrationDto);

        assertEquals(USERNAME, result.getUsername());
        verify(userRepository).save(user);
    }

    @Test
    void register_whenEmailExists_shouldThrowEmailAlreadyExistsException() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.register(registrationDto));
    }

    @Test
    void register_whenUsernameExists_shouldThrowUserAlreadyExistsException() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.register(registrationDto));
    }

    @Test
    void login_withValidCredentials_shouldReturnToken() {
        when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtTokenizer.getAuthToken(any(), any(), any())).thenReturn("mockToken");

        String token = userService.login(loginDto);

        assertEquals("mockToken", token);
    }

    @Test
    void login_withInvalidCredentials_shouldThrowBadCredentialsException() {
        when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class, () -> userService.login(loginDto), "Should throw for non-existent user");

        when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(false);
        assertThrows(BadCredentialsException.class, () -> userService.login(loginDto), "Should throw for incorrect password");
    }

    @Test
    void findUserByUsername_shouldReturnUserOrEmpty() {
        when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
        Optional<ApplicationUser> foundResult = userService.findUserByUsername(USERNAME);
        assertTrue(foundResult.isPresent());
        assertEquals(USERNAME, foundResult.get().getUsername());

        when(userRepository.findUserByUsername("nonexistent")).thenReturn(Optional.empty());
        Optional<ApplicationUser> notFoundResult = userService.findUserByUsername("nonexistent");
        assertFalse(notFoundResult.isPresent());
    }

    @Test
    void deleteUserByUsername_withExistingUser_shouldSucceed() {
        ApplicationUser deletedUser = ApplicationUser.builder()
            .id(-100L)
            .username("deleted user")
            .password("123459876543216789")
            .role(Role.CUSTOMER)
            .email("")
            .build();

        when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(userRepository.findUserByUsername("deleted user")).thenReturn(Optional.of(deletedUser));

        userService.deleteUserByUsername(USERNAME);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUserByUsername_whenUserNotFound_shouldThrowNotFoundException() {
        when(userRepository.findUserByUsername("nonexistent")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.deleteUserByUsername("nonexistent"));
    }

    @Test
    void loadUserByUsername_whenUserNotFound_shouldThrowUsernameNotFoundException() {
        when(userRepository.findUserByUsername("nonexistent")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("nonexistent"));
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetailsWithCorrectRoles_forAdminUser() {
        user.setRole(Role.ADMIN);
        when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));

        UserDetails userDetails = userService.loadUserByUsername(USERNAME);

        assertEquals(USERNAME, userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER")));
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_WORKER")));
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetailsWithCorrectRoles_forWorkerUser() {
        user.setRole(Role.WORKER);
        when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));

        UserDetails userDetails = userService.loadUserByUsername(USERNAME);

        assertEquals(USERNAME, userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_WORKER")));
        assertEquals(1, userDetails.getAuthorities().size());
    }

    @Test
    void update_withAuthorizedUser_shouldSucceed() {
        setupSecurityContext(USERNAME);
        when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(userRepository.save(any(ApplicationUser.class))).thenReturn(user);

        userService.update(updateDto);

        verify(userMapper).updateUserFromDto(updateDto, user);
        verify(userRepository).save(user);
    }

    @Test
    void update_whenUserIsDifferent_shouldThrowAccessDeniedException() {
        setupSecurityContext(USERNAME);
        when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));

        UserUpdateDto differentUserUpdateDto = UserUpdateDto.builder()
            .username("anotheruser")
            .email("another_new@example.com")
            .firstName("Jane")
            .lastName("S")
            .area("Salzburg")
            .build();
        when(userRepository.findUserByUsername("anotheruser")).thenReturn(Optional.of(anotherUser));

        assertThrows(AccessDeniedException.class, () -> userService.update(differentUserUpdateDto));
    }

    @Test
    void update_whenUserToUpdateIsNotFound_shouldThrowNotFoundException() {
        setupSecurityContext(USERNAME);
        when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));

        UserUpdateDto nonexistentUserUpdateDto = UserUpdateDto.builder()
            .username("nonexistent")
            .firstName("No")
            .lastName("One")
            .email("noone@example.com")
            .build();
        when(userRepository.findUserByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.update(nonexistentUserUpdateDto));
    }

    @Test
    void update_byAdminUser_shouldSucceedForAnyUser() {
        setupSecurityContext(adminUser.getUsername());
        when(userRepository.findUserByUsername(adminUser.getUsername())).thenReturn(Optional.of(adminUser));

        UserUpdateDto targetUserUpdateDto = UserUpdateDto.builder()
            .username(anotherUser.getUsername())
            .firstName("JaneUpdated")
            .lastName("SmithUpdated")
            .email("another_updated@example.com")
            .build();
        when(userRepository.findUserByUsername(anotherUser.getUsername())).thenReturn(Optional.of(anotherUser));
        when(userRepository.save(any(ApplicationUser.class))).thenReturn(anotherUser);

        userService.update(targetUserUpdateDto);

        verify(userMapper).updateUserFromDto(targetUserUpdateDto, anotherUser);
        verify(userRepository).save(anotherUser);
    }

    @Test
    void getUserByUserNameForEdit_shouldReturnDtoOrThrow() {

        UserUpdateDetailDto detailDto = new UserUpdateDetailDto();
        when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(userMapper.applicationUserToUserUpdateDetailDto(user)).thenReturn(detailDto);
        UserUpdateDetailDto result = userService.getUserByUserNameForEdit(USERNAME);
        assertEquals(detailDto, result);

        when(userRepository.findUserByUsername("nonexistent")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.getUserByUserNameForEdit("nonexistent"));
    }

    @Test
    void getUserDetailsById_shouldReturnDtoOrThrow() {

        UserDetailDto detailDto = new UserDetailDto();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.applicationUserToUserDetailDto(user)).thenReturn(detailDto);
        UserDetailDto result = userService.getUserDetailsById(1L);
        assertEquals(detailDto, result);


        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.getUserDetailsById(99L));
    }

    @Test
    void getCurrentUser_shouldReturnUserFromSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(authentication.getPrincipal()).thenReturn(USERNAME);
        when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));

        ApplicationUser currentUser = userService.getCurrentUser();

        assertNotNull(currentUser);
        assertEquals(USERNAME, currentUser.getUsername());
    }

    @Test
    void getCurrentUser_whenUserFromContextIsNotFound_shouldThrowNotFoundException() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(authentication.getPrincipal()).thenReturn("ghost");
        when(userRepository.findUserByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getCurrentUser());
    }

    @Test
    void getAllUsersByPage_withValidParameters_shouldReturnPageDto() {
        // Setup
        int offset = 0;
        int limit = 10;
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by("username").ascending());
        Page<ApplicationUser> userPage = new PageImpl<>(Collections.singletonList(user), pageable, 1);

        when(userRepository.findByRoleNotAndUsernameNot(eq(Role.ADMIN), any(), any(Pageable.class))).thenReturn(userPage);
        when(userMapper.applicationUsersToUserListDto(anyList())).thenReturn(
            Collections.singletonList(new UserListDto())
        );

        // Execute
        PageDto<UserListDto> result = userService.getAllUsersByPage(offset, limit);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(userRepository).findByRoleNotAndUsernameNot(eq(Role.ADMIN), any(), any(Pageable.class));
    }

    @Test
    void banUser_withValidId_shouldSetBannedTrue() {
        // Setup
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Execute
        userService.banUser(userId);

        // Verify
        assertTrue(user.isBanned());
        verify(userRepository).save(user);
    }

    @Test
    void banUser_withInvalidId_shouldThrowNotFoundException() {
        // Setup
        Long invalidId = 99L;
        when(userRepository.findById(invalidId)).thenReturn(Optional.empty());

        // Execute & Verify
        assertThrows(NotFoundException.class, () -> userService.banUser(invalidId));
    }

    @Test
    void unbanUser_withValidId_shouldSetBannedFalse() {
        // Setup
        Long userId = 1L;
        user.setBanned(true); // Start as banned
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Execute
        userService.unbanUser(userId);

        // Verify
        assertFalse(user.isBanned());
        verify(userRepository).save(user);
    }

    @Test
    void unbanUser_withInvalidId_shouldThrowNotFoundException() {
        // Setup
        Long invalidId = 99L;
        when(userRepository.findById(invalidId)).thenReturn(Optional.empty());

        // Execute & Verify
        assertThrows(NotFoundException.class, () -> userService.unbanUser(invalidId));
    }

    @Test
    void searchUsers_withUsernamePart_shouldReturnFilteredResults() {
        // Setup
        String usernamePart = "test";
        int offset = 0;
        int limit = 10;
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by("username").ascending());
        Page<ApplicationUser> userPage = new PageImpl<>(Collections.singletonList(user), pageable, 1);

        when(userRepository.findByUsernameContainingIgnoreCaseAndRoleNotAndUsernameNot(
            eq(usernamePart), eq(Role.ADMIN), any() , any(Pageable.class)
        )).thenReturn(userPage);
        when(userMapper.applicationUsersToUserListDto(anyList())).thenReturn(
            Collections.singletonList(new UserListDto())
        );

        // Execute
        PageDto<UserListDto> result = userService.searchUsers(usernamePart, offset, limit);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(userRepository).findByUsernameContainingIgnoreCaseAndRoleNotAndUsernameNot(
            eq(usernamePart), eq(Role.ADMIN), any(), any(Pageable.class)
        );
    }

    // Helper method to set up SecurityContext mock
    private void setupSecurityContext(String username) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getPrincipal()).thenReturn(username);
    }
}