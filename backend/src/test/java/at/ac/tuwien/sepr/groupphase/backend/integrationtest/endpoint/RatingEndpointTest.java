package at.ac.tuwien.sepr.groupphase.backend.integrationtest.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.basetest.DatabaseCleanup;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.rating.RatingDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.rating.RatingStatsDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobOffer;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.entity.Rating;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobOfferRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.RatingRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.type.JobOfferStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.JobStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
    import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class RatingEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private RatingRepository ratingRepository;
    @Autowired
    private DatabaseCleanup cleanup;

    private ApplicationUser customer;
    private ApplicationUser worker;
    private JobRequest jobRequest;
    private JobOffer jobOffer;
    private RatingDto validRatingDto;
    private final String RATING_BASE_URI = "/api/v1/ratings";

    @BeforeEach
    public void setup() {

        cleanup.clearAll();

        customer = new ApplicationUser();
        customer.setEmail("customer@test.com");
        customer.setUsername("test customer");
        customer.setPassword("password");
        customer.setRole(Role.CUSTOMER);
        customer.setBanned(false);
        customer = userRepository.save(customer);

        worker = new ApplicationUser();
        worker.setEmail("worker@test.com");
        worker.setUsername("test worker");
        worker.setPassword("password");
        worker.setRole(Role.WORKER);
        worker.setBanned(false);
        worker = userRepository.save(worker);


        jobRequest = new JobRequest();
        jobRequest.setCustomer(customer);
        jobRequest.setTitle("Test Job");
        jobRequest.setStatus(JobStatus.DONE);
        jobRequest = jobRequestRepository.save(jobRequest);


        jobOffer = new JobOffer();
        jobOffer.setJobRequest(jobRequest);
        jobOffer.setWorker(worker);
        jobOffer.setStatus(JobOfferStatus.DONE);
        jobOffer = jobOfferRepository.save(jobOffer);


        validRatingDto = new RatingDto();
        validRatingDto.setStars(5);
        validRatingDto.setComment("Great work!");

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("test customer", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
            )
        );

    }


    @Test
    public void getRatingByRequestId_existingRating_returnsRating() throws Exception {

        Rating rating = new Rating();
        rating.setFromUser(customer);
        rating.setToUser(worker);
        rating.setJobRequest(jobRequest);
        rating.setStars(4);
        rating.setComment("Good job");
        ratingRepository.save(rating);


        mockMvc.perform(get(RATING_BASE_URI + "/" + jobRequest.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(result -> {
                RatingDto response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    RatingDto.class
                );
                assertEquals(4, response.getStars());
                assertEquals("Good job", response.getComment());
            });
    }

    @Test
    public void getRatingByRequestId_noRating_returnsNoContent() throws Exception {
        mockMvc.perform(get(RATING_BASE_URI + "/" + jobRequest.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());
    }

    @Test
    public void createRating_validData_returnsCreated() throws Exception {
        mockMvc.perform(post(RATING_BASE_URI + "/" + jobRequest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRatingDto)))
            .andExpect(status().isOk());

        List<Rating> ratings = ratingRepository.findAll();
        assertEquals(1, ratings.size());
        assertEquals(5, ratings.getFirst().getStars());
        assertEquals("Great work!", ratings.getFirst().getComment());
    }

    @Test
    public void updateRating_existingRating_updatesSuccessfully() throws Exception {

        Rating rating = new Rating();
        rating.setFromUser(customer);
        rating.setToUser(worker);
        rating.setJobRequest(jobRequest);
        rating.setStars(3);
        rating.setComment("Average");
        ratingRepository.save(rating);


        RatingDto updateDto = new RatingDto();
        updateDto.setStars(5);
        updateDto.setComment("Excellent!");


        mockMvc.perform(put(RATING_BASE_URI + "/" + jobRequest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk());


        Rating updated = ratingRepository.findByFromUserIdAndJobRequestId(customer.getId(), jobRequest.getId())
            .orElseThrow();
        assertEquals(5, updated.getStars());
        assertEquals("Excellent!", updated.getComment());
    }

    @Test
    public void getLatestRatings_returnsRecentRatings() throws Exception {

        createRatings(worker, 12);


        MvcResult result = mockMvc.perform(get(RATING_BASE_URI + "/user/" + worker.getId()))
            .andExpect(status().isOk())
            .andReturn();

        RatingDto[] ratings = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            RatingDto[].class
        );
        assertEquals(10, ratings.length);
        assertEquals(5, ratings[0].getStars());
    }

    @Test
    public void getRatingStats_calculatesAverageAndCount() throws Exception {

        createRatings(worker, 3);


        MvcResult result = mockMvc.perform(get(RATING_BASE_URI + "/user/" + worker.getId() + "/stats"))
            .andExpect(status().isOk())
            .andReturn();

        RatingStatsDto stats = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            RatingStatsDto.class
        );
        assertEquals(3, stats.getCount());
        assertEquals(5.0f, stats.getAverage(), 0.01);
    }


    private void createRatings(ApplicationUser toUser, int count) {
        for (int i = 0; i < count; i++) {

            JobRequest newRequest = new JobRequest();
            newRequest.setCustomer(customer);
            newRequest.setTitle("Test Job " + i);
            newRequest.setStatus(JobStatus.DONE);
            newRequest = jobRequestRepository.save(newRequest);


            JobOffer newOffer = new JobOffer();
            newOffer.setJobRequest(newRequest);
            newOffer.setWorker(worker);
            newOffer.setStatus(JobOfferStatus.DONE);
            jobOfferRepository.save(newOffer);

            Rating rating = new Rating();
            rating.setFromUser(customer);
            rating.setToUser(toUser);
            rating.setJobRequest(newRequest);
            rating.setStars(5);
            rating.setComment("Rating " + i);
            rating.setCreatedAt(LocalDateTime.now().minusMinutes(count - i));
            ratingRepository.save(rating);
        }
    }



    @Test
    public void updateRating_nonExistingRating_returnsBadRequest() throws Exception {

        RatingDto updateDto = new RatingDto();
        updateDto.setStars(4);
        updateDto.setComment("Trying to update non-existing rating");

        mockMvc.perform(put(RATING_BASE_URI + "/69")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void getLatestRatings_noRatings_returnsEmptyList() throws Exception {

        mockMvc.perform(get(RATING_BASE_URI + "/user/" + worker.getId()))
            .andExpect(status().isOk())
            .andExpect(result -> {
                RatingDto[] ratings = objectMapper.readValue(
                    result.getResponse().getContentAsString(), RatingDto[].class);
                assertEquals(0, ratings.length);
            });
    }

    @Test
    public void getRatingStats_noRatings_returnsZeroCountAndAverage() throws Exception {

        MvcResult result = mockMvc.perform(get(RATING_BASE_URI + "/user/" + worker.getId() + "/stats"))
            .andExpect(status().isOk())
            .andReturn();

        RatingStatsDto stats = objectMapper.readValue(
            result.getResponse().getContentAsString(), RatingStatsDto.class);

        assertEquals(0, stats.getCount());
        assertEquals(0.0f, stats.getAverage(), 0.01);
    }


}