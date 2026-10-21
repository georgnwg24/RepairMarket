package at.ac.tuwien.sepr.groupphase.backend.integrationtest.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.basetest.DatabaseCleanup;
import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyEditRestDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.Property;
import at.ac.tuwien.sepr.groupphase.backend.repository.PropertyRepository;
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

@SpringBootTest
@WebAppConfiguration
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PropertyEndpointTest implements TestData {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private DatabaseCleanup cleanup;

    private static final String PROPERTY_URI = "/api/v1/properties";

    private Long prop1Id;
    private Long prop2Id;
    private Long prop3Id;

    @BeforeEach
    public void setupSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("vince", "12345678", List.of())
        );
    }

    @BeforeEach
    public void setup() {
        cleanup.clearAll();

        ApplicationUser vince = ApplicationUser.builder()
            .username("vince")
            .password("encodedPassword")
            .email("vince@example.com")
            .role(Role.CUSTOMER)
            .build();
        vince = userRepository.save(vince);

        ApplicationUser ned = ApplicationUser.builder()
            .username("ned")
            .password("encodedPassword")
            .email("ned@example.com")
            .role(Role.CUSTOMER)
            .build();
        ned = userRepository.save(ned);

        Property prop1 = Property.builder()
            .customer(vince)
            .address("Address")
            .area("Area")
            .countryCode("AT")
            .postalCode("1234")
            .latitude(48.2f)
            .longitude(16.3f)
            .build();
        prop1 = propertyRepository.save(prop1);
        prop1Id = prop1.getId();

        Property prop2 = Property.builder()
            .customer(vince)
            .address("Address")
            .area("Area")
            .countryCode("AT")
            .postalCode("1234")
            .latitude(48.3f)
            .longitude(16.4f)
            .build();
        prop2 = propertyRepository.save(prop2);
        prop2Id = prop2.getId();

        Property prop3 = Property.builder()
            .customer(ned)
            .address("Address")
            .area("Area")
            .countryCode("AT")
            .postalCode("1234")
            .latitude(48.4f)
            .longitude(16.5f)
            .build();
        prop3 = propertyRepository.save(prop3);
        prop3Id = prop3.getId();
    }

    @Test
    @WithMockUser(username = "vince", roles = {"CUSTOMER"})
    public void getCustomerProperties_shouldReturnList() throws Exception {
        mockMvc.perform(get(PROPERTY_URI)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "vince", roles = {"CUSTOMER"})
    public void getPropertyById_whenValidId_shouldReturnProperty() throws Exception {
        mockMvc.perform(get(PROPERTY_URI + "/" + prop1Id)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "ned", roles = {"CUSTOMER"})
    public void getPropertyById_whenInvalidId_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get(PROPERTY_URI + "/99999")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "vince", roles = {"CUSTOMER"})
    public void createProperty_whenValid_shouldReturnCreated() throws Exception {
        PropertyCreateDto newProperty = new PropertyCreateDto();
        newProperty.setAddress("Test Street 123");
        newProperty.setArea("Nice Area");
        newProperty.setCountryCode("AT");
        newProperty.setPostalCode("6971");

        mockMvc.perform(post(PROPERTY_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProperty)))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "vince", roles = {"CUSTOMER"})
    public void createProperty_whenInvalid_shouldReturnBadRequest() throws Exception {
        PropertyEditRestDto invalidProperty = new PropertyEditRestDto();
        invalidProperty.setAddress("");
        invalidProperty.setArea("");
        invalidProperty.setCountryCode("");
        invalidProperty.setPostalCode("");
        mockMvc.perform(post(PROPERTY_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProperty)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "ned", roles = {"CUSTOMER"})
    public void updateProperty_whenValid_shouldReturnOk() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ned", "12345678", List.of())
        );
        PropertyEditRestDto update = new PropertyEditRestDto();
        update.setAddress("Corn Street");
        update.setArea("Mushroom Kingdom");
        update.setCountryCode("UK");
        update.setPostalCode("1100");

        mockMvc.perform(put(PROPERTY_URI + "/" + prop3Id + "/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "vince", roles = {"CUSTOMER"})
    public void updateProperty_whenInvalid_shouldReturnBadRequest() throws Exception {
        PropertyEditRestDto invalidUpdate = new PropertyEditRestDto();
        invalidUpdate.setAddress("");
        invalidUpdate.setArea("");
        invalidUpdate.setCountryCode("");
        invalidUpdate.setPostalCode("");
        mockMvc.perform(put(PROPERTY_URI + "/" + prop2Id + "/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUpdate)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "vince", roles = {"CUSTOMER"})
    public void deleteProperty_whenValid_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete(PROPERTY_URI + "/" + prop2Id)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "ned", roles = {"CUSTOMER"})
    public void deleteProperty_whenNotAuthorizes_shouldReturnUnAuth() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ned", "12345678", List.of())
        );
        mockMvc.perform(delete(PROPERTY_URI + "/" + prop1Id)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(username = "vince", roles = {"CUSTOMER"})
    public void updateProperty_whenNotOwner_shouldReturnForbidden() throws Exception {
        PropertyEditRestDto update = new PropertyEditRestDto();
        update.setAddress("New Address");
        update.setArea("New Area");
        update.setCountryCode("US");
        update.setPostalCode("90210");

        mockMvc.perform(put(PROPERTY_URI + "/" + prop3Id + "/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "vince", roles = {"CUSTOMER"})
    public void createProperty_whenMissingRequiredField_shouldReturnBadRequest() throws Exception {
        PropertyCreateDto invalidProperty = new PropertyCreateDto();
        invalidProperty.setAddress(null);
        invalidProperty.setArea("Vienna");
        invalidProperty.setCountryCode("AT");
        invalidProperty.setPostalCode("1100");

        mockMvc.perform(post(PROPERTY_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProperty)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "vince", roles = {"CUSTOMER"})
    public void lookupArea_whenValid_shouldReturnArea() throws Exception {
        mockMvc.perform(get(PROPERTY_URI + "/lookup-area")
                .param("postalCode", "1234")
                .param("countryCode", "AT")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.areaNames").isArray());
    }


}
