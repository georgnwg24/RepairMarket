package at.ac.tuwien.sepr.groupphase.backend.integrationtest.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.basetest.DatabaseCleanup;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.RatingRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReportRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


@ActiveProfiles("test")
@SpringBootTest
@EnableWebMvc
@WebAppConfiguration
@AutoConfigureMockMvc
@Transactional
public class ReportEndpointTest {

    private static final String REPORT_URI = "/api/v1/reports";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenizer jwtTokenizer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private DatabaseCleanup cleanup;

    private ApplicationUser reporterUser;
    private String reporterToken;
    private ApplicationUser adminUser;
    private String adminToken;

    private String getAuthTokenFor(String username, List<String> roles, Long id) {
        return jwtTokenizer.getAuthToken(username, roles, id);
    }

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    private JobRequest jobRequest;

    @BeforeEach
    public void setup() {

        cleanup.clearAll();

        reporterUser = new ApplicationUser();
        reporterUser.setEmail("reporter@test.com");
        reporterUser.setUsername("alfred");
        reporterUser.setPassword("password");
        reporterUser.setRole(Role.CUSTOMER);
        reporterUser.setBanned(false);
        reporterUser = userRepository.save(reporterUser);
        reporterToken = getAuthTokenFor(reporterUser.getUsername(), List.of("ROLE_CUSTOMER"), reporterUser.getId());

        // Create an admin user for admin-specific tests
        adminUser = new ApplicationUser();
        adminUser.setEmail("admin@test.com");
        adminUser.setUsername("admin");
        adminUser.setPassword("password");
        adminUser.setRole(Role.ADMIN);
        adminUser.setBanned(false);
        adminUser = userRepository.save(adminUser);
        adminToken = getAuthTokenFor(adminUser.getUsername(), List.of("ROLE_ADMIN"), adminUser.getId());


        jobRequest = new JobRequest();
        jobRequest.setCustomer(reporterUser);
        jobRequest.setTitle("Sample JobRequest");
        jobRequest.setDescription("Test description");
        jobRequest.setCategory(Category.FLOORING);
        jobRequest.setStatus(JobStatus.PENDING);
        jobRequest = jobRequestRepository.save(jobRequest);
    }


    @Test
    public void createReport_whenValid_shouldReturnCreatedReport() throws Exception {
        JsonObject reportData = new JsonObject();
        reportData.addProperty("jobRequestId", jobRequest.getId());
        reportData.addProperty("type", "JOB_REQUEST");
        reportData.addProperty("reason", "This is a test report");

        mockMvc.perform(post(REPORT_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reportData.toString())
                .header("Authorization", reporterToken)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.reason").value("This is a test report"));
    }



    @Test
    public void createReport_whenMissingAuthorization_shouldReturnForbidden() throws Exception {
        JsonObject reportData = new JsonObject();
        reportData.addProperty("targetId", 123L);
        reportData.addProperty("message", "Unauthorized report");

        mockMvc.perform(post(REPORT_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reportData.toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }



    @Test
    public void createReport_whenServiceThrowsException_shouldReturnBadRequest() throws Exception {
        JsonObject reportData = new JsonObject();
        reportData.addProperty("jobRequestId", jobRequest.getId());
        reportData.addProperty("type", "JOB_REQUEST");
        reportData.addProperty("reason", "Force failure");

        jobRequestRepository.deleteAll(); // trigger service exception

        mockMvc.perform(post(REPORT_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reportData.toString())
                .header("Authorization", reporterToken)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void getMyReports_shouldReturnOkAndEmptyList() throws Exception {
        mockMvc.perform(get(REPORT_URI + "/me")
                .header("Authorization", reporterToken))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void getAllReports_shouldReturnOkAndPageDto() throws Exception {
        mockMvc.perform(get(REPORT_URI)
                        .param("offset", "0")
                        .param("limit", "5")
                        .param("status", "true")
                        .header("Authorization", adminToken)) // Use adminToken
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    public void getReportsByTarget_whenNotFound_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get(REPORT_URI + "/target/999999")
                .header("Authorization", reporterToken))
            .andExpect(status().isNotFound());
    }

    @Test
    public void getReportDetail_whenNotFound_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get(REPORT_URI + "/999999")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void closeReport_whenNotFound_shouldReturnNotFound() throws Exception {
        mockMvc.perform(post(REPORT_URI + "/999999/close")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void openReport_whenNotFound_shouldReturnNotFound() throws Exception {
        mockMvc.perform(post(REPORT_URI + "/999999/open")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void reportMessage_whenInvalid_shouldReturnBadRequest() throws Exception {
        JsonObject invalidMessage = new JsonObject(); // intentionally empty

        mockMvc.perform(post(REPORT_URI + "/messages")
                .header("Authorization", reporterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidMessage.toString()))
            .andExpect(status().isBadRequest());
    }


}