package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyEditDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.AreaLookupDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.PropertyMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.Property;
import at.ac.tuwien.sepr.groupphase.backend.exception.ActiveJobRequestsExistException;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PropertyRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.PropertyService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.geolocation.GeonamesApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.HttpClientErrorException;
import java.io.IOException;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PropertyServiceImpl implements PropertyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final JobRequestRepository jobRequestRepository;
    private final PropertyMapper propertyMapper;
    private final ObjectMapper objectMapper;

    private final UserService userService;



    public PropertyServiceImpl(PropertyRepository propertyRepository, UserRepository userRepository, JobRequestRepository jobRequestRepository, PropertyMapper propertyMapper, UserService userService, ObjectMapper objectMapper) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.jobRequestRepository = jobRequestRepository;

        this.propertyMapper = propertyMapper;
        this.objectMapper = objectMapper;

        this.userService = userService;
    }

    @Override
    public List<PropertyListDto> listProperties() {
        String username = userService.getCurrentUser().getUsername();
        LOGGER.trace("listProperties() with parameters: {}", username);
        try {
            ApplicationUser user = userRepository.findUserByUsername(username).orElseThrow(() -> {
                LOGGER.error("Error while fetching User: {}", username);
                return new NotFoundException();
            });
            Long id = user.getId();
            return propertyMapper.propertyToListDto(propertyRepository.findAllByCustomerId(id));
        } catch (NotFoundException e) {
            LOGGER.warn("Properties with ID {} not found, throwing exception", username);
            throw new NotFoundException("Properties couldn't be found");
        }
    }

    @Override
    public PropertyCreateDto create(PropertyCreateDto createDto) {
        String username = userService.getCurrentUser().getUsername();
        LOGGER.trace("create() with parameters: {} {}", createDto, username);
        ApplicationUser user = userRepository.findUserByUsername(username).orElseThrow(() -> {
            LOGGER.error("Error while fetching: {}", username);
            return new NotFoundException();
        });

        Property property = propertyMapper.createDtoToProperty(createDto);
        property.setCustomer(user);

        updatePropertyWithGeonamesData(property);

        propertyRepository.save(property);
        return createDto;
    }

    @Override
    public PropertyEditDto update(PropertyEditDto updateDto) {
        ApplicationUser user = userService.getCurrentUser();
        LOGGER.trace("update() with parameters: {} {}", updateDto, user.getUsername());
        Property property = propertyRepository.findById(updateDto.getId()).orElseThrow(() -> {
            LOGGER.error("Error while updating Property: {}", updateDto);
            return new NotFoundException();
        });
        if (!property.getCustomer().getId().equals(user.getId())) {
            LOGGER.error("User '{}' is not authorized to update Property with id{}", user.getUsername(), updateDto.getId());
            throw new AccessDeniedException("Not Authorized");
        }

        if (jobRequestRepository.existsByPropertyId(updateDto.getId())) {
            throw new ActiveJobRequestsExistException();
        }

        property.setCountryCode(updateDto.getCountryCode());
        property.setPostalCode(updateDto.getPostalCode());
        property.setArea(updateDto.getArea());
        property.setAddress(updateDto.getAddress());

        updatePropertyWithGeonamesData(property);

        property.setCustomer(user);
        propertyRepository.save(property);
        return updateDto;
    }

    @Override
    public PropertyDetailDto getPropertyById(Long id) {
        LOGGER.trace("getPropertyById() with parameters: {}", id);
        return propertyMapper.propertyToDetailDto(propertyRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Property not found")));
    }

    @Override
    public void deleteById(Long id) {
        ApplicationUser user = userService.getCurrentUser();
        LOGGER.trace("deleteById() with parameters: {}", id);
        ApplicationUser authorizedUser = propertyRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("No user with id: " + id))
            .getCustomer();

        if (!user.getId().equals(authorizedUser.getId())) {
            LOGGER.error("User '{}' is not authorized to delete Property with id {}", user.getUsername(), id);
            throw new AccessDeniedException("You are not Authorized");
        }

        if (jobRequestRepository.existsByPropertyId(id)) {
            throw new ActiveJobRequestsExistException();
        }
        propertyRepository.deleteById(id);

    }

    private void updatePropertyWithGeonamesData(Property property) {
        try {
            String jsonResponse = fetchGeonamesDataFromApi(property.getPostalCode(), property.getCountryCode(), property.getArea());
            if (jsonResponse != null) {
                GeonamesApiResponse apiResponse = objectMapper.readValue(jsonResponse, GeonamesApiResponse.class);

                if (apiResponse != null && apiResponse.getResults() != null && !apiResponse.getResults().isEmpty()) {
                    var result = apiResponse.getResults().get(0);
                    if (result != null) {
                        property.setLatitude(result.getLatitude());
                        property.setLongitude(result.getLongitude());
                        LOGGER.info("Geocoded property for zipcode {}: area='{}', lat={}, lon={}",
                            property.getPostalCode(), property.getArea(), property.getLatitude(), property.getLongitude());
                    }
                } else {
                    LOGGER.warn("Geonames API returned no records for zipcode: {} and country: {}", property.getPostalCode(), property.getCountryCode());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to parse Geonames API response for zipcode {}: {}", property.getPostalCode(), e.getMessage());
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred during geocoding for zipcode {}: {}", property.getPostalCode(), e.getMessage());
        }
    }

    private String fetchGeonamesDataFromApi(String postalCode, String countryCode, String area) throws IOException {
        String whereClause = "";
        if (area.isEmpty()) {
            whereClause = String.format("postal_code='%s' and country_code='%s'", postalCode, countryCode);
        } else {
            whereClause = String.format("postal_code='%s' and country_code='%s' and place_name='%s'", postalCode, countryCode, area);
        }

        String url = UriComponentsBuilder
            .fromHttpUrl("https://data.opendatasoft.com/api/explore/v2.1/catalog/datasets/geonames-postal-code@public/records")
            .queryParam("where", whereClause)
            .build()
            .toUriString();

        //LOGGER.info("Calling Geonames API v2.1: {}", url);
        RestTemplate restTemplate = new RestTemplate();
        try {
            return restTemplate.getForObject(url, String.class);
        } catch (HttpClientErrorException e) {
            LOGGER.warn("Could not fetch data from Geonames API for postalCode {}: {} {}", postalCode, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        }
    }

    @Override
    public AreaLookupDto lookupArea(String postalCode, String countryCode) {
        List<String> areaNames = new ArrayList<>();
        try {
            String jsonResponse = fetchGeonamesDataFromApi(postalCode, countryCode, "");
            if (jsonResponse != null) {
                GeonamesApiResponse apiResponse = objectMapper.readValue(jsonResponse, GeonamesApiResponse.class);

                if (apiResponse != null && apiResponse.getResults() != null && !apiResponse.getResults().isEmpty()) {
                    areaNames = apiResponse.getResults().stream()
                        .map(result -> result.getPlaceName())
                        .filter(name -> name != null && !name.isEmpty())
                        .collect(Collectors.toList());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to parse Geonames API response for lookup with postalCode {}: {}", postalCode, e.getMessage());
            // In case of error, returns an empty list
        }
        return new AreaLookupDto(areaNames);
    }
}
