package at.ac.tuwien.sepr.groupphase.backend.integrationtest.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.basetest.DatabaseCleanup;
import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserUpdateRestDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.RatingRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@WebAppConfiguration
@AutoConfigureMockMvc
public class UserEndpointTest implements TestData {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DatabaseCleanup cleanup;

    private ApplicationUser worker;
    private ApplicationUser customer;
    private ApplicationUser admin;

    @BeforeEach
    public void setup() {

        cleanup.clearAll();

        worker = new ApplicationUser();
        worker.setUsername("testworker");
        worker.setPassword("password");
        worker.setEmail("testworker@example.com");
        worker.setRole(Role.WORKER);
        worker = userRepository.save(worker);

        customer = new ApplicationUser();
        customer.setUsername("testcustomer");
        customer.setPassword("password");
        customer.setEmail("testcustomer@example.com");
        customer.setRole(Role.CUSTOMER);
        customer = userRepository.save(customer);

        admin = new ApplicationUser();
        admin.setUsername("admin");
        admin.setPassword("password");
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);
        admin = userRepository.save(admin);

        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken("admin", null, List.of());

        SecurityContextHolder.getContext().setAuthentication(auth);

    }

    private void authenticateAs(String username, String role) {
        var auth = new UsernamePasswordAuthenticationToken(username, null, List.of(() -> "ROLE_" + role));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void getUserForEdit_whenWorkerAuthenticated_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/users/edit"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    @WithMockUser(username = "testworker", roles = {"WORKER"})
    public void updateUser_whenValidInput_shouldReturnUpdatedUser() throws Exception {
        var updateDto = UserUpdateRestDto.builder()
            .email("test@example.com")
            .firstName("Test")
            .lastName("User")
            .countryCode("AT")
            .postalCode("1010")
            .area("Vienna")
            .build();

        mockMvc.perform(put("/api/v1/users/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(username = "testworker", roles = {"WORKER"})
    public void getUserById_whenValidId_shouldReturnUser() throws Exception {
        authenticateAs("testworker", "WORKER");
        mockMvc.perform(get("/api/v1/users/" + worker.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testworker"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void listAllUsers_whenAdmin_shouldReturnUserList() throws Exception {
        authenticateAs("admin", "ADMIN");
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").exists());
    }


    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void banUser_whenAdmin_shouldReturnNoContent() throws Exception {
        authenticateAs("admin", "ADMIN");
        mockMvc.perform(patch("/api/v1/users/" + worker.getId() + "/ban"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void unbanUser_whenAdmin_shouldReturnNoContent() throws Exception {
        authenticateAs("admin", "ADMIN");
        mockMvc.perform(patch("/api/v1/users/" + worker.getId() + "/unban"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "notadmin", roles = {"ADMIN"})
    public void deleteUser_whenNotAdmin_shouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/users/testcustomer"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testcustomer", roles = {"CUSTOMER"})
    public void updateUser_whenInvalidEmail_shouldReturnBadRequest() throws Exception {
        var updateDto = UserUpdateRestDto.builder()
            .email("invalid-email")
            .firstName("Test")
            .lastName("User")
            .countryCode("AT")
            .postalCode("1010")
            .area("Vienna")
            .build();

        mockMvc.perform(put("/api/v1/users/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testworker", roles = {"WORKER"})
    public void updateUser_whenRequiredFieldsMissing_shouldReturnBadRequest() throws Exception {
        var updateDto = UserUpdateRestDto.builder()
            .email("")
            .postalCode("")
            .area("")
            .build();

        mockMvc.perform(put("/api/v1/users/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isBadRequest());
    }
}
