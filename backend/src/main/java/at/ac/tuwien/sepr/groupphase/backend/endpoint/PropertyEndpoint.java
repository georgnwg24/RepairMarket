package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyEditRestDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.AreaLookupDto;

import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.service.PropertyService;
import jakarta.annotation.security.PermitAll;

import java.util.List;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


import java.lang.invoke.MethodHandles;

@RestController
@RequestMapping(path = PropertyEndpoint.BASE_PATH)
public class PropertyEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final String BASE_PATH = "/api/v1/properties";
    private final PropertyService propertyService;


    @Autowired
    public PropertyEndpoint(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @PermitAll
    @GetMapping()
    public List<PropertyListDto> getCustomerProperties() throws NotFoundException {
        LOGGER.info("GET " + BASE_PATH);
        return propertyService.listProperties();
    }

    @PermitAll
    @GetMapping(path = "/{id}")
    public PropertyDetailDto getProperty(@PathVariable("id") Long id) throws NotFoundException {
        LOGGER.info("GET " + BASE_PATH + "/{}", id);
        return propertyService.getPropertyById(id);
    }

    @PermitAll
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> create(
        @Valid @RequestBody PropertyCreateDto toCreate,
        BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(bindingResult.getAllErrors());
        }

        LOGGER.info("POST " + BASE_PATH);
        LOGGER.debug("Received JSON: {}", toCreate);
        LOGGER.debug("Body of request:\n{}", toCreate);
        PropertyCreateDto created = propertyService.create(toCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);

    }

    @PermitAll
    @PutMapping(path = "/{id}/edit")
    public ResponseEntity<?> update(
        @PathVariable("id") Long id,
        @Valid @RequestBody PropertyEditRestDto toUpdate,
        BindingResult bindingResult) {
        LOGGER.info("PUT " + BASE_PATH + "/{}, {}", id, toUpdate);
        LOGGER.debug("Received JSON: {}", toUpdate);
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(bindingResult.getAllErrors());
        }
        LOGGER.info("PUT " + BASE_PATH);
        LOGGER.debug("Body of request:\n{}", toUpdate);
        return ResponseEntity.ok(propertyService.update(toUpdate.updateWithId(id)));
    }

    @PermitAll
    @DeleteMapping(path = "/{id}")
    public ResponseEntity<?> deleteById(
        @PathVariable("id") Long id
    ) throws NotFoundException {
        LOGGER.info("DELETE " + BASE_PATH + "/{}", id);
        propertyService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Looks up an area name based on postal code and country.
     * Used for autofilling forms on the frontend.
     *
     * @param postalCode The postal code to look up.
     * @param countryCode    The 2-3-letter country code.
     * @return An AreaLookupDto containing the found area name.
     */
    @PermitAll
    @GetMapping("/lookup-area")
    public AreaLookupDto lookupArea(
        @RequestParam String postalCode,
        @RequestParam String countryCode
    ) {
        LOGGER.info("GET /lookup-area from postalCode={}, country={}", postalCode, countryCode);
        return propertyService.lookupArea(postalCode, countryCode);
    }

}
