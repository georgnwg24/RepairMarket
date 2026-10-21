package at.ac.tuwien.sepr.groupphase.backend.integrationtest.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobOffer;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.entity.Property;
import at.ac.tuwien.sepr.groupphase.backend.repository.ChatRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobOfferRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PropertyRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.type.Category;
import at.ac.tuwien.sepr.groupphase.backend.type.JobOfferStatus;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@SpringBootTest
@AutoConfigureMockMvc
@WebAppConfiguration
@ActiveProfiles({"test","datagen"})
@Transactional
public class ChatEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private ChatRepository chatRepository;

    private static final String BASE_PATH = "/api/v1/chats";

    private long jobRequestId;
    private RequestPostProcessor customerAuth;

    @BeforeEach
    void setUp() {
        var worker = ApplicationUser.builder()
            .role(Role.WORKER).username("worker").password("12345678").email("worker@test.com").build();
        userRepository.save(worker);
        var customer = ApplicationUser.builder()
            .role(Role.CUSTOMER).username("customer").password("12345678").email("customer@test.com").build();
        userRepository.save(customer);

        var property = Property.builder()
            .customer(customer).address("Hof 552").area("Schwarzenberg")
            .latitude(47.41415f).longitude(9.85154f).postalCode("6867").countryCode("AT").build();
        propertyRepository.save(property);

        var jobRequest = JobRequest.builder()
            .customer(customer).deadline(LocalDate.now().plusMonths(1))
            .category(Category.OTHER).title("jobRequest")
            .createdAt(LocalDateTime.now()).property(property).build();
        var savedRequest = jobRequestRepository.save(jobRequest);
        jobRequestId = savedRequest.getId();

        var jobOffer = JobOffer.builder()
            .jobRequest(savedRequest).worker(worker)
            .createdAt(LocalDateTime.now().plusDays(2))
            .comment("good price").status(JobOfferStatus.ACCEPTED).build();
        jobOfferRepository.save(jobOffer);

        customerAuth = authentication(
            new UsernamePasswordAuthenticationToken(
                "customer", null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
            )
        );
    }

    @Test
    void engageChatAndExpect_201IsCreated() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/engage/" + jobRequestId)
                .with(customerAuth)
                .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.chatId").isNumber());
    }

    @Test
    void getMessages_whenParticipant_returnsMessages() throws Exception {
        var res = mockMvc.perform(post(BASE_PATH + "/engage/" + jobRequestId)
                .with(customerAuth)
                .with(csrf()))
            .andReturn();
        Integer chatId = JsonPath.read(res.getResponse().getContentAsString(), "$.chatId");

        mockMvc.perform(get(BASE_PATH + "/" + chatId.longValue() + "/messages").with(customerAuth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deleteConversation_whenParticipant_deletesChat() throws Exception {
        var res = mockMvc.perform(post(BASE_PATH + "/engage/" + jobRequestId)
                .with(customerAuth)
                .with(csrf()))
            .andReturn();
        Integer chatId = JsonPath.read(res.getResponse().getContentAsString(), "$.chatId");

        mockMvc.perform(delete(BASE_PATH + "/" + chatId)
                .with(customerAuth)
                .with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(chatRepository.findById((long) chatId)).isEmpty();
    }

    @Test
    void uploadImage_whenNotImplemented_returns201() throws Exception {
        var file = new MockMultipartFile("image","test.jpg",MediaType.IMAGE_JPEG_VALUE,"content".getBytes());
        mockMvc.perform(multipart(BASE_PATH + "/uploads").file(file)
                .with(customerAuth)
                .with(csrf()))
            .andExpect(status().isCreated());
    }
}
