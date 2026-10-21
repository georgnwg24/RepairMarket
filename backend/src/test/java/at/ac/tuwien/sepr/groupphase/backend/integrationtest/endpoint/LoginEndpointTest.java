package at.ac.tuwien.sepr.groupphase.backend.integrationtest.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserLoginDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegistrationDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.EmailAlreadyExistsException;
import at.ac.tuwien.sepr.groupphase.backend.exception.UserAlreadyExistsException;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;


/**
 * Tests for the Login REST API endpoint.
 */
@ActiveProfiles({"test", "datagen"})
@SpringBootTest
@EnableWebMvc
@WebAppConfiguration
@Transactional
public class LoginEndpointTest {

    @Autowired
    private WebApplicationContext webAppContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UserRegistrationDto validRegistrationDto;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).build();

        validRegistrationDto = UserRegistrationDto.builder()
            .username("TestPenguin")
            .password("testPassword")
            .email("testPenguin@google.com")
            .role(Role.CUSTOMER)
            .firstName("Test")
            .lastName("Penguin")
            .area("Antarctica")
            .confirmPassword("testPassword")
            .build();
    }

    @Test
    void loginWithValidCredentialsReturnsTokenAnd200() throws Exception {
        // Register a user first
        mockMvc.perform(post("/api/v1/authentication/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationDto)))
            .andExpect(status().isCreated());

        // Create login credentials
        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setUsername(validRegistrationDto.getUsername());
        loginDto.setPassword(validRegistrationDto.getPassword());

        // Attempt login
        mockMvc.perform(post("/api/v1/authentication")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isString()); // Check token is returned
    }

    @Test
    void loginWithInvalidPasswordReturns401() throws Exception {
        // Register a user
        mockMvc.perform(post("/api/v1/authentication/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationDto)))
            .andExpect(status().isCreated());

        // Invalid login credentials
        UserLoginDto invalidLoginDto = new UserLoginDto();
        invalidLoginDto.setUsername(validRegistrationDto.getUsername());
        invalidLoginDto.setPassword("wrongPassword");

        // Attempt login
        mockMvc.perform(post("/api/v1/authentication")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLoginDto)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithNonExistingUsernameReturns401() throws Exception {
        // Non-existent credentials
        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setUsername("nonExistentUser");
        loginDto.setPassword("anyPassword");

        // Attempt login
        mockMvc.perform(post("/api/v1/authentication")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isUnauthorized());
    }

    /*
    @Test
    void loginWithBannedUserReturns403() throws Exception {
        // Register a user
        mockMvc.perform(post("/api/v1/authentication/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationDto)))
            .andExpect(status().isCreated());

        // Ban the user (directly via repository for test setup)
        userRepository.findUserByUsername(validRegistrationDto.getUsername())
            .ifPresent(user -> {
                user.setBanned(true);
                userRepository.save(user);
            });

        // Valid login credentials for banned user
        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setUsername(validRegistrationDto.getUsername());
        loginDto.setPassword(validRegistrationDto.getPassword());

        // Attempt login
        mockMvc.perform(post("/api/v1/authentication")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isForbidden());
    } */

    @Test
    void register201Created() throws Exception {
            mockMvc.perform(post("/api/v1/authentication/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("TestPenguin"))
                .andExpect(jsonPath("$.email").value("testPenguin@google.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("Penguin"))
                .andExpect(jsonPath("$.area").value("Antarctica"))
                // .andExpect(jsonPath("$.password").doesNotExist())
            ;
        }

    @Test
    public void registerWithInvalidDataReturns400() throws Exception {
        UserRegistrationDto invalidDto = new UserRegistrationDto();

        mockMvc.perform(post("/api/v1/authentication/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
            .andExpect(status().isBadRequest());
    }


    @Test
    public void registerWithExistingUsernameReturns409() throws Exception {
        // First registration
        mockMvc.perform(post("/api/v1/authentication/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationDto)))
            .andExpect(status().isCreated());

        // Second registration with same username
        UserRegistrationDto duplicateDto = UserRegistrationDto.builder()
            .username(validRegistrationDto.getUsername())  // same username
            .email("different@email.com")                 // different email
            .password("testPassword")
            .confirmPassword("testPassword")
            .role(Role.CUSTOMER)
            .firstName("Test")
            .lastName("Penguin")
            .area("Antarctica")
            .build();

        mockMvc.perform(post("/api/v1/authentication/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateDto)))
            .andExpect(status().isConflict())
            .andExpect(result -> assertTrue(result.getResolvedException()
                instanceof UserAlreadyExistsException));
    }

    @Test
    public void registerWithExistingEmailReturns409() throws Exception {
        // First registration
        mockMvc.perform(post("/api/v1/authentication/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationDto)))
            .andExpect(status().isCreated());

        // Second registration with same email
        UserRegistrationDto duplicateDto = UserRegistrationDto.builder()
            .username("DifferentUsername")                 // different username
            .email(validRegistrationDto.getEmail())        // same email
            .password("testPassword")
            .confirmPassword("testPassword")
            .role(Role.CUSTOMER)
            .firstName("Test")
            .lastName("Penguin")
            .area("Antarctica")
            .build();

        mockMvc.perform(post("/api/v1/authentication/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateDto)))
            .andExpect(status().isConflict())
            .andExpect(result -> assertTrue(result.getResolvedException()
                instanceof EmailAlreadyExistsException));
    }

    @Test
    public void registerWorkerWithoutAddressReturns400() throws Exception {
        UserRegistrationDto workerDto = UserRegistrationDto.builder()
            .username("TestWorker")
            .password("workerPass")
            .confirmPassword("workerPass")
            .email("worker@test.com")
            .role(Role.WORKER)
            .firstName("Worker")
            .lastName("Test")
            // Missing address fields
            .build();

        mockMvc.perform(post("/api/v1/authentication/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(workerDto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$[0].defaultMessage")
                .value("Country, Postal Code, and Area are required for workers."));
    }

    @Test
    public void registerWithPasswordMismatchReturns400() throws Exception {
        UserRegistrationDto mismatchDto = UserRegistrationDto.builder()
            .username(validRegistrationDto.getUsername())
            .password(validRegistrationDto.getPassword())
            .confirmPassword("differentPassword")  // Mismatch
            .email(validRegistrationDto.getEmail())
            .role(validRegistrationDto.getRole())
            .firstName(validRegistrationDto.getFirstName())
            .lastName(validRegistrationDto.getLastName())
            .area(validRegistrationDto.getArea())
            .build();

        mockMvc.perform(post("/api/v1/authentication/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mismatchDto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$[0].defaultMessage")
                .value("Passwords do not match"));
    }
}
