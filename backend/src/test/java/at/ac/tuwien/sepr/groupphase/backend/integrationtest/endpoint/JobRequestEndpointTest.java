package at.ac.tuwien.sepr.groupphase.backend.integrationtest.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.basetest.DatabaseCleanup;
import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import at.ac.tuwien.sepr.groupphase.backend.type.Category;
import at.ac.tuwien.sepr.groupphase.backend.type.JobStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import com.google.gson.JsonObject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@EnableWebMvc
@WebAppConfiguration
@AutoConfigureMockMvc
@Transactional
public class JobRequestEndpointTest implements TestData {

    private static final String JOB_REQUEST_URI = "/api/v1/job-requests";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenizer jwtTokenizer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    @Autowired
    private DatabaseCleanup cleanup;

    private ApplicationUser customer;
    private String customerToken;

    private String getAuthTokenFor(String username, List<String> roles, Long id) {
        return jwtTokenizer.getAuthToken(username, roles, id);
    }

    @BeforeEach
    public void setup() {

        cleanup.clearAll();

        customer = new ApplicationUser();
        customer.setEmail("customer@test.com");
        customer.setUsername("customer");
        customer.setPassword("password");
        customer.setRole(Role.CUSTOMER);
        customer.setBanned(false);
        customer = userRepository.save(customer);

        customerToken = getAuthTokenFor(customer.getUsername(), List.of("ROLE_CUSTOMER"), customer.getId());
    }

    @Test
    public void createJobRequest_whenValid_shouldReturnCreatedRequest() throws Exception {
        JsonObject jobRequestJson = new JsonObject();
        jobRequestJson.addProperty("title", "Fix my floor");
        jobRequestJson.addProperty("description", "Need floor fixed urgently.");
        jobRequestJson.addProperty("category", "FLOORING");
        jobRequestJson.addProperty("status", "PENDING");
        jobRequestJson.addProperty("deadline", LocalDate.now().plusDays(1).toString());

        mockMvc.perform(post(JOB_REQUEST_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", customerToken)
                .content(jobRequestJson.toString()))
            .andExpect(status().isCreated());
    }

    @Test
    public void createJobRequest_whenMissingTitle_shouldReturnBadRequest() throws Exception {
        JsonObject jobRequestJson = new JsonObject();
        jobRequestJson.addProperty("description", "Missing title");
        jobRequestJson.addProperty("category", "FLOORING");
        jobRequestJson.addProperty("deadline", LocalDate.now().plusDays(7).toString());

        mockMvc.perform(post(JOB_REQUEST_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", customerToken)
                .content(jobRequestJson.toString()))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void getJobRequest_whenExists_shouldReturnRequest() throws Exception {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setTitle("Test Request");
        jobRequest.setDescription("Test Desc");
        jobRequest.setCategory(Category.FLOORING);
        jobRequest.setDeadline(LocalDate.now().plusDays(5));
        jobRequest.setCustomer(customer);
        jobRequest.setStatus(JobStatus.PENDING);
        jobRequest = jobRequestRepository.save(jobRequest);

        mockMvc.perform(get(JOB_REQUEST_URI + "/" + jobRequest.getId())
                .header("Authorization", customerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Test Request"))
            .andExpect(jsonPath("$.description").value("Test Desc"));
    }

    @Test
    public void getJobRequest_whenNotFound_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get(JOB_REQUEST_URI + "/999999")
                .header("Authorization", customerToken))
            .andExpect(status().isNotFound());
    }

    @Test
    public void deleteJobRequest_whenExists_shouldReturnNoContent() throws Exception {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setTitle("To Delete");
        jobRequest.setDescription("Delete me");
        jobRequest.setCategory(Category.FLOORING);
        jobRequest.setDeadline(LocalDate.now().plusDays(5));
        jobRequest.setCustomer(customer);
        jobRequest.setStatus(JobStatus.PENDING);
        jobRequest = jobRequestRepository.save(jobRequest);

        mockMvc.perform(delete(JOB_REQUEST_URI + "/" + jobRequest.getId())
                .header("Authorization", customerToken))
            .andExpect(status().isNoContent());
    }

    @Test
    public void updateJobRequest_whenValid_shouldReturnUpdated() throws Exception {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setTitle("Old Title");
        jobRequest.setDescription("Old Desc");
        jobRequest.setCategory(Category.FLOORING);
        jobRequest.setDeadline(LocalDate.now().plusDays(5));
        jobRequest.setCustomer(customer);
        jobRequest.setStatus(JobStatus.PENDING);
        jobRequest = jobRequestRepository.save(jobRequest);

        JsonObject updateRequestJson = new JsonObject();
        updateRequestJson.addProperty("title", "Updated title");
        updateRequestJson.addProperty("description", "Updated description");
        updateRequestJson.addProperty("category", "FLOORING");
        updateRequestJson.addProperty("status", "PENDING");
        updateRequestJson.addProperty("deadline", LocalDate.now().plusDays(1).toString());
        mockMvc.perform(put(JOB_REQUEST_URI + "/" + jobRequest.getId() + "/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", customerToken)
                .content(updateRequestJson.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated title"))
            .andExpect(jsonPath("$.description").value("Updated description"));
    }


    @Test
    public void listAllJobRequests_shouldReturnOkAndList() throws Exception {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setTitle("List All Test");
        jobRequest.setDescription("For list all test");
        jobRequest.setCategory(Category.FLOORING);
        jobRequest.setDeadline(LocalDate.now().plusDays(5));
        jobRequest.setCustomer(customer);
        jobRequest.setStatus(JobStatus.PENDING);
        jobRequestRepository.save(jobRequest);

        mockMvc.perform(get(JOB_REQUEST_URI + "/all")
                .header("Authorization", customerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].title").value("List All Test"));
    }

    @Test
    public void searchCustomerJobRequests_shouldReturnOk() throws Exception {
        mockMvc.perform(get(JOB_REQUEST_URI + "/user/search")
                .header("Authorization", customerToken)
                .param("offset", "0")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

}
