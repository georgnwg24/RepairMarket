package at.ac.tuwien.sepr.groupphase.backend.unittests.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyEditDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.PropertyMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.Property;
import at.ac.tuwien.sepr.groupphase.backend.exception.ActiveJobRequestsExistException;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PropertyRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.PropertyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JobRequestRepository jobRequestRepository;
    @Mock
    private PropertyMapper propertyMapper;
    @Mock
    private UserService userService;

    @InjectMocks
    private PropertyServiceImpl propertyService;

    private ApplicationUser user;
    private PropertyCreateDto createDto;
    private PropertyEditDto editDto;
    private Property property;

    @BeforeEach
    void setUp() {
        user = ApplicationUser.builder()
            .id(1L)
            .username("john")
            .build();

        property = Property.builder()
            .id(1L)
            .customer(user)
            .area("Vienna")
            .address("Reumannplatz 1")
            .countryCode("AT")
            .postalCode("1100")
            .build();

        createDto = PropertyCreateDto.builder()
            .area("Vienna")
            .address("Reumannplatz 1")
            .countryCode("AT")
            .postalCode("1100")
            .build();

        editDto = PropertyEditDto.builder()
            .id(1L)
            .area("Bern")
            .address("Effingerstrasse 21")
            .countryCode("Swiss")
            .postalCode("3008")
            .build();
    }

    @Test
    void listProperties_shouldReturnUserProperties() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(userRepository.findUserByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(propertyRepository.findAllByCustomerId(user.getId())).thenReturn(List.of(property));
        when(propertyMapper.propertyToListDto(List.of(property))).thenReturn(List.of(new PropertyListDto()));

        var resultWithProperty = propertyService.listProperties();
        assertNotNull(resultWithProperty);
        assertEquals(1, resultWithProperty.size());

        when(propertyRepository.findAllByCustomerId(user.getId())).thenReturn(Collections.emptyList());
        when(propertyMapper.propertyToListDto(Collections.emptyList())).thenReturn(Collections.emptyList());

        var resultWithoutProperties = propertyService.listProperties();
        assertNotNull(resultWithoutProperties);
        assertTrue(resultWithoutProperties.isEmpty());
    }

    @Test
    void listProperties_userNotFound_shouldThrowNotFoundException() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(userRepository.findUserByUsername(user.getUsername())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> propertyService.listProperties());
    }

    @Test
    void create_withValidData_shouldSaveAndReturnDto() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(userRepository.findUserByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(propertyMapper.createDtoToProperty(createDto)).thenReturn(new Property());
        when(propertyRepository.save(any(Property.class))).thenReturn(property);

        PropertyCreateDto result = propertyService.create(createDto);

        assertEquals(createDto, result);
        verify(propertyRepository).save(any(Property.class));
    }

    @Test
    void create_userNotFound_shouldThrowNotFoundException() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(userRepository.findUserByUsername(user.getUsername())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> propertyService.create(createDto));
    }

    @Test
    void update_withValidData_shouldSaveAndReturnDto() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(propertyRepository.findById(editDto.getId())).thenReturn(Optional.of(property));
        when(jobRequestRepository.existsByPropertyId(editDto.getId())).thenReturn(false);
        when(propertyRepository.save(any(Property.class))).thenReturn(property);

        PropertyEditDto result = propertyService.update(editDto);

        assertEquals(editDto, result);
        verify(propertyRepository).save(property);
    }

    @Test
    void update_unauthorizedUser_shouldThrowAccessDeniedException() {
        ApplicationUser otherUser = ApplicationUser.builder().id(999L).username("other").build();
        property.setCustomer(otherUser);

        when(userService.getCurrentUser()).thenReturn(user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        assertThrows(AccessDeniedException.class, () -> propertyService.update(editDto));
    }

    @Test
    void update_withActiveJobRequests_shouldThrowException() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(jobRequestRepository.existsByPropertyId(1L)).thenReturn(true);

        assertThrows(ActiveJobRequestsExistException.class, () -> propertyService.update(editDto));
    }

    @Test
    void update_propertyNotFound_shouldThrowNotFoundException() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(propertyRepository.findById(editDto.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> propertyService.update(editDto));
    }

    @Test
    void getById_withExistingId_shouldReturnDetailDto() {
        PropertyDetailDto detailDto = new PropertyDetailDto();
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(propertyMapper.propertyToDetailDto(property)).thenReturn(detailDto);

        var result = propertyService.getPropertyById(1L);

        assertEquals(detailDto, result);
    }

    @Test
    void getById_propertyNotFound_shouldThrowNotFoundException() {
        when(propertyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> propertyService.getPropertyById(999L));
    }



    @Test
    void delete_withValidData_shouldSucceed() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(jobRequestRepository.existsByPropertyId(1L)).thenReturn(false);

        propertyService.deleteById(1L);

        verify(propertyRepository).deleteById(1L);
    }

    @Test
    void delete_propertyNotFound_shouldThrowNotFoundException() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> propertyService.deleteById(1L));
    }

    @Test
    void delete_unauthorizedUser_shouldThrowAccessDeniedException() {
        ApplicationUser otherUser = ApplicationUser.builder().id(999L).build();
        property.setCustomer(otherUser);

        when(userService.getCurrentUser()).thenReturn(user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        assertThrows(AccessDeniedException.class, () -> propertyService.deleteById(1L));
    }

    @Test
    void delete_withActiveJobRequests_shouldThrowException() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(jobRequestRepository.existsByPropertyId(1L)).thenReturn(true);

        assertThrows(ActiveJobRequestsExistException.class, () -> propertyService.deleteById(1L));
    }
}