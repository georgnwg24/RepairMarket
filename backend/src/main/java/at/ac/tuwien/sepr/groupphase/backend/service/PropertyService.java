package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyEditDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.PropertyListDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.property.AreaLookupDto;

import java.util.List;

public interface PropertyService {

    /**
     * Retrieves a list of all properties stored in the system.
     *
     * @return a list representing all properties
     */
    List<PropertyListDto> listProperties();

    /**
     * Creates a new property.
     *
     * @param property the data transfer object containing property creation data
     * @return the created property
     */
    PropertyCreateDto create(PropertyCreateDto property);

    /**
     * Updates an existing property.
     *
     * @param property the data transfer object containing updated property data
     * @return the updated property
     */
    PropertyEditDto update(PropertyEditDto property);

    /**
     * Retrieves detailed information about a property by its ID.
     *
     * @param id the ID of the property to retrieve
     * @return detailed property information
     */
    PropertyDetailDto getPropertyById(Long id);

    /**
     * Deletes a property identified by its ID.
     *
     * @param id the ID of the property to delete
     */
    void deleteById(Long id);

    AreaLookupDto lookupArea(String postalCode, String countryCode);
}
