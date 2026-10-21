package at.ac.tuwien.sepr.groupphase.backend.integrationtest.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyEditDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyListDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.ActiveJobRequestsExistException;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.service.PropertyService;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles({"test", "datagen"})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Transactional
public class PropertyServiceTest {

    @Autowired
    private PropertyService propertyService;

    @BeforeEach
    void setupSecurityContext() {
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken("ned", "12345678");
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void listProperties_shouldReturnNonEmptyList() {
        List<?> properties = propertyService.listProperties();
        assertNotNull(properties);
        assertFalse(properties.isEmpty());
    }

    @Test
    void createProperty_whenValid_shouldReturnCreateDto() {
        PropertyCreateDto createDto = new PropertyCreateDto();
        createDto.setAddress("Test Street 123");
        createDto.setArea("Vienna");

        PropertyCreateDto returned = propertyService.create(createDto);

        assertNotNull(returned);
        assertEquals("Test Street 123", returned.getAddress());
    }

    @Test
    void getPropertyById_whenExisting_shouldReturnDetailDto() {
        PropertyCreateDto createDto = new PropertyCreateDto();
        createDto.setAddress("GetById Street");
        createDto.setArea("Vienna");
        propertyService.create(createDto);

        List<PropertyListDto> properties = propertyService.listProperties();
        PropertyListDto someProperty = properties.stream()
            .filter(p -> p.getAddress().equals("GetById Street"))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Property not found in list"));

        PropertyDetailDto detailDto = propertyService.getPropertyById(someProperty.getId());

        assertNotNull(detailDto);
        assertEquals("GetById Street", detailDto.getAddress());
    }

    @Test
    void getPropertyById_whenNonExisting_shouldThrowNotFoundException() {
        assertThrows(NotFoundException.class, () -> propertyService.getPropertyById(99999L));
    }


    @Test
    void deleteProperty_whenOwner_shouldDelete() {
        PropertyCreateDto createDto = new PropertyCreateDto();
        createDto.setAddress("Delete Me Street");
        createDto.setArea("Vienna");
        propertyService.create(createDto);

        List<PropertyListDto> properties = propertyService.listProperties();
        PropertyListDto toDelete = properties.stream()
            .filter(p -> p.getAddress().equals("Delete Me Street"))
            .findFirst()
            .orElseThrow();

        assertDoesNotThrow(() -> propertyService.deleteById(toDelete.getId()));

        assertThrows(NotFoundException.class, () -> propertyService.getPropertyById(toDelete.getId()));
    }

    @Test
    void deleteProperty_whenActiveJobRequestsExist_shouldThrowActiveJobRequestsExistException() {
        long propertyIdWithActiveJobs = -106L;

        assertThrows(ActiveJobRequestsExistException.class, () -> propertyService.deleteById(propertyIdWithActiveJobs));
    }

    @Test
    void updateProperty_whenOwner_shouldUpdateAndPersistChanges() {
        PropertyCreateDto createDto = new PropertyCreateDto();
        createDto.setAddress("Original Address");
        createDto.setPostalCode("1010");
        createDto.setCountryCode("AT");
        createDto.setArea("Vienna");
        propertyService.create(createDto);

        PropertyListDto propertyToUpdate = propertyService.listProperties().stream()
            .filter(p -> p.getAddress().equals("Original Address"))
            .findFirst()
            .orElseThrow();

        PropertyEditDto editDto = new PropertyEditDto();
        editDto.setId(propertyToUpdate.getId());
        editDto.setAddress("Updated Address");
        editDto.setArea("New Area");
        editDto.setPostalCode("1020");
        editDto.setCountryCode("AT");

        propertyService.update(editDto);

        PropertyDetailDto updatedProperty = propertyService.getPropertyById(propertyToUpdate.getId());
        assertNotNull(updatedProperty);
        assertEquals("Updated Address", updatedProperty.getAddress());
        assertEquals("New Area", updatedProperty.getArea());
        assertEquals("1020", updatedProperty.getPostalCode());
    }

    @Test
    void updateProperty_whenNotOwner_shouldThrowAccessDeniedException() {
        long propertyIdOfOtherUser = -101L;

        PropertyEditDto editDto = new PropertyEditDto();
        editDto.setId(propertyIdOfOtherUser);
        editDto.setAddress("Trying to update");

        assertThrows(AccessDeniedException.class, () -> propertyService.update(editDto));
    }

    @Test
    void updateProperty_whenActiveJobsExist_shouldThrowException() {
        long propertyIdWithActiveJobs = -106L;

        PropertyEditDto editDto = new PropertyEditDto();
        editDto.setId(propertyIdWithActiveJobs);
        editDto.setAddress("Trying to update");

        assertThrows(ActiveJobRequestsExistException.class, () -> propertyService.update(editDto));
    }

    @Test
    void deleteProperty_whenNotOwner_shouldThrowAccessDeniedException() {
        long propertyIdOfOtherUser = -101L;

        assertThrows(AccessDeniedException.class, () -> propertyService.deleteById(propertyIdOfOtherUser));
    }

    @Test
    void createProperty_withValidAddress_shouldSetGeocoordinates() {
        PropertyCreateDto createDto = new PropertyCreateDto();
        createDto.setAddress("Stephansplatz 1");
        createDto.setPostalCode("1010");
        createDto.setCountryCode("AT");
        createDto.setArea("Wien, Innere Stadt");

        propertyService.create(createDto);

        PropertyListDto createdProperty = propertyService.listProperties().stream()
            .filter(p -> "Stephansplatz 1".equals(p.getAddress()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Test property not found after creation"));

        PropertyDetailDto detailDto = propertyService.getPropertyById(createdProperty.getId());

        assertNotNull(detailDto.getLatitude(), "Latitude should be set by geocoding service");
        assertNotNull(detailDto.getLongitude(), "Longitude should be set by geocoding service");
    }

    @Test
    void lookupArea_withValidPostalCode_shouldReturnAreas() {
        var result = propertyService.lookupArea("1010", "AT");
        assertNotNull(result);
        assertNotNull(result.getAreaNames());
        assertFalse(result.getAreaNames().isEmpty(), "Expected to find area names for a valid postal code");
        assertTrue(result.getAreaNames().getFirst().contains("Wien"));
    }

    @Test
    void lookupArea_withInvalidPostalCode_shouldReturnEmptyList() {
        var result = propertyService.lookupArea("9999999", "XX");
        assertNotNull(result);
        assertNotNull(result.getAreaNames());
        assertTrue(result.getAreaNames().isEmpty(), "Expected no area names for an invalid postal code");
    }
}
