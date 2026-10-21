package at.ac.tuwien.sepr.groupphase.backend.integrationtest.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.basetest.DatabaseCleanup;
import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobOffer;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobOfferRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.RatingRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import at.ac.tuwien.sepr.groupphase.backend.type.Category;
import at.ac.tuwien.sepr.groupphase.backend.type.JobOfferStatus;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
public class JobOfferEndpointTest implements TestData {

    private static final String JOB_OFFER_URI = "/api/v1/job-offers";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenizer jwtTokenizer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;


    @Autowired
    private DatabaseCleanup cleanup;

    private ApplicationUser customer;
    private ApplicationUser worker;
    private JobRequest jobRequest;
    private JobOffer jobOffer;

    private String workerToken;
    private String customerToken;

    private String getAuthTokenFor(String username, List<String> roles, Long id) {
        return jwtTokenizer.getAuthToken(username, roles, id);
    }

    @BeforeEach
    public void setup() {
        cleanup.clearAll();

        // Instead of creating new users, find the users loaded by the test profile.
        // This avoids the DataIntegrityViolationException.
        customer = userRepository.findUserByUsername("vince")
            .orElseGet(() -> {
                ApplicationUser newUser = new ApplicationUser();
                newUser.setEmail("vince@test.com");
                newUser.setUsername("vince");
                newUser.setPassword("password");
                newUser.setRole(Role.CUSTOMER);
                newUser.setBanned(false);
                return userRepository.save(newUser);
            });

        worker = userRepository.findUserByUsername("ethan")
            .orElseGet(() -> {
                ApplicationUser newUser = new ApplicationUser();
                newUser.setEmail("ethan@test.com");
                newUser.setUsername("ethan");
                newUser.setPassword("password");
                newUser.setRole(Role.WORKER);
                newUser.setBanned(false);
                return userRepository.save(newUser);
            });

        jobRequest = new JobRequest();
        jobRequest.setCustomer(customer);
        jobRequest.setTitle("Test Job");
        jobRequest.setDescription("Test job description");
        jobRequest.setCategory(Category.FLOORING);
        jobRequest.setStatus(JobStatus.PENDING);
        jobRequest.setDeadline(LocalDate.now().plusDays(10));
        jobRequest = jobRequestRepository.save(jobRequest);

        jobOffer = new JobOffer();
        jobOffer.setJobRequest(jobRequest);
        jobOffer.setWorker(worker);
        jobOffer.setStatus(JobOfferStatus.PENDING);
        jobOffer.setPrice(100.0f);
        jobOffer = jobOfferRepository.save(jobOffer);


        workerToken = getAuthTokenFor(worker.getUsername(), List.of("ROLE_WORKER"), worker.getId());
        customerToken = getAuthTokenFor(customer.getUsername(), List.of("ROLE_CUSTOMER"), customer.getId());
    }



    @Test
    public void createJobOffer_whenValid_shouldReturnCreatedOffer() throws Exception {

        jobOfferRepository.deleteAll();

        JsonObject offerData = new JsonObject();
        offerData.addProperty("price", 150.0);
        offerData.addProperty("comment", "I can do this by next week.");

        MockMultipartFile offerJson = new MockMultipartFile(
            "jobOffer", "", "application/json", offerData.toString().getBytes()
        );

        mockMvc.perform(multipart(JOB_OFFER_URI + "/" + jobRequest.getId() + "/offers")
                .file(offerJson)
                .with(req -> { req.setMethod("POST"); return req; })
                .header("Authorization", workerToken)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.price").value(150.0))
            .andExpect(jsonPath("$.comment").value("I can do this by next week."));
    }

    @Test
    public void createJobOffer_whenUserNotWorker_shouldReturUnAuthorized() throws Exception {
        JsonObject offerData = new JsonObject();
        offerData.addProperty("price", 100.0);

        MockMultipartFile offerJson = new MockMultipartFile(
            "jobOffer", "", "application/json", offerData.toString().getBytes()
        );

        mockMvc.perform(multipart(JOB_OFFER_URI + "/" + jobRequest.getId() + "/offers")
                .file(offerJson)
                .with(req -> { req.setMethod("POST"); return req; })
                .header("Authorization", customerToken)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void createJobOffer_whenJobRequestNotExist_shouldReturnNotFound() throws Exception {
        JsonObject offerData = new JsonObject();
        offerData.addProperty("price", 200.0);

        MockMultipartFile offerJson = new MockMultipartFile(
            "jobOffer", "", "application/json", offerData.toString().getBytes()
        );

        mockMvc.perform(multipart(JOB_OFFER_URI + "/999999/offers")
                .file(offerJson)
                .with(req -> { req.setMethod("POST"); return req; })
                .header("Authorization", workerToken)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void createJobOffer_whenInvalidPrice_shouldReturnBadRequest() throws Exception {
        JsonObject offerData = new JsonObject();
        offerData.addProperty("price", -10.0);

        MockMultipartFile offerJson = new MockMultipartFile(
            "jobOffer", "", "application/json", offerData.toString().getBytes()
        );

        mockMvc.perform(multipart(JOB_OFFER_URI + "/" + jobRequest.getId() + "/offers")
                .file(offerJson)
                .with(req -> { req.setMethod("POST"); return req; })
                .header("Authorization", workerToken)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
            .andExpect(status().isBadRequest());
    }


    @Test
    public void getOffersForCustomer_whenAuthenticated_shouldReturnOk() throws Exception {
        mockMvc.perform(get(JOB_OFFER_URI + "/customer")
                .header("Authorization", customerToken)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    public void withdrawJobOffer_whenValid_shouldReturnOk() throws Exception {
        mockMvc.perform(post(JOB_OFFER_URI + "/" + jobOffer.getId() + "/withdraw")
                .header("Authorization", workerToken))
            .andExpect(status().isOk());
    }


    @Test
    public void withdrawJobOffer_whenInvalidId_shouldReturnNotFound() throws Exception {
        mockMvc.perform(post(JOB_OFFER_URI + "/999999/withdraw")
                .header("Authorization", workerToken))
            .andExpect(status().isNotFound());
    }

    @Test
    public void createJobOffer_whenMissingAuthorization_shouldReturnForbidden() throws Exception {

        JsonObject offerData = new JsonObject();
        offerData.addProperty("price", 120.0);

        MockMultipartFile offerJson = new MockMultipartFile(
            "jobOffer", "", "application/json", offerData.toString().getBytes()
        );

        mockMvc.perform(multipart(JOB_OFFER_URI + "/" + jobRequest.getId() + "/offers")
                .file(offerJson)
                .with(req -> { req.setMethod("POST"); return req; })
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
            .andExpect(status().isForbidden());
    }

    @Test
    public void createJobOffer_whenUsingGetMethod_shouldReturnMethodNotAllowed() throws Exception {

        mockMvc.perform(get(JOB_OFFER_URI + "/" + jobRequest.getId() + "/offers")
                .header("Authorization", workerToken))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void getOfferById_whenOfferExists_shouldReturnOffer() throws Exception {
        mockMvc.perform(get(JOB_OFFER_URI + "/" + jobOffer.getId())
                .header("Authorization", workerToken))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.price").value(jobOffer.getPrice()));
    }

    @Test
    public void getOfferById_whenOfferDoesNotExist_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get(JOB_OFFER_URI + "/999999")
                .header("Authorization", workerToken))
            .andExpect(status().isNotFound());
    }



    @Test
    public void updateJobOffer_whenValid_shouldReturnOk() throws Exception {
        JsonObject updateData = new JsonObject();
        updateData.addProperty("price", 199.99);
        updateData.addProperty("comment", "Updated offer comment");

        MockMultipartFile offerJson = new MockMultipartFile(
            "jobOffer", "", "application/json", updateData.toString().getBytes()
        );

        mockMvc.perform(multipart(JOB_OFFER_URI + "/" + jobOffer.getId())
                .file(offerJson)
                .with(req -> { req.setMethod("PUT"); return req; })
                .header("Authorization", workerToken)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
            .andExpect(status().isOk());
    }

    @Test
    public void updateJobOffer_whenInvalid_shouldReturnBadRequest() throws Exception {
        JsonObject updateData = new JsonObject();
        updateData.addProperty("price", -50.0);

        MockMultipartFile offerJson = new MockMultipartFile(
            "jobOffer", "", "application/json", updateData.toString().getBytes()
        );

        mockMvc.perform(multipart(JOB_OFFER_URI + "/" + jobOffer.getId())
                .file(offerJson)
                .with(req -> { req.setMethod("PUT"); return req; })
                .header("Authorization", workerToken)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
            .andExpect(status().isBadRequest());
    }



}