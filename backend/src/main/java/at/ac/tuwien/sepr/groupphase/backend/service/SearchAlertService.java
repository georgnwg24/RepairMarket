package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.searchalert.SearchAlertCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.searchalert.SearchAlertDetailDto;

import java.util.List;

public interface SearchAlertService {

    /**
     * Create a new Search Alert for the logged-in user.
     *
     * @param dto the dto with the params
     */
    void createAlert(SearchAlertCreateDto dto);

    /**
     * Check if the logged-in user already saved the given Search Alert.
     *
     * @param dto the dto with the params
     * @return true if duplicate, otherwise false
     */
    boolean alertIsDuplicate(SearchAlertCreateDto dto);

    /**
     * Returns a list of all Search Alerts of the logged-in user.
     *
     * @return a list of {@link SearchAlertDetailDto} Dtos.
     */
    List<SearchAlertDetailDto> getUserAlerts();

    /**
     * Deletes a Search alert from the persistent data store.
     *
     * @param id the ID of the Search Alert to be deleted.
     */
    void deleteAlert(Long id);

    /**
     * Updates the active status of a Search Alert, determining if push requests should be sent on new offers.
     *
     * @param id     the ID of the Search Alert
     * @param active the new active status
     */
    void updateAlertStatus(Long id, boolean active);

    /**
     * Sets the count of the alert to 0 (used when the worker clicks on the Search Alert).
     *
     * @param id the ID of the Search Alert
     */
    void resetAlertCount(Long id);
}
