package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.searchalert.SearchAlertCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.searchalert.SearchAlertDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.service.SearchAlertService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;
import java.util.List;

@RestController
@RequestMapping(path = SearchAlertEndpoint.BASE_PATH)
public class SearchAlertEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String BASE_PATH = "/api/v1/search-alerts";
    private final SearchAlertService searchAlertService;

    @Autowired
    public SearchAlertEndpoint(SearchAlertService searchAlertService) {
        this.searchAlertService = searchAlertService;
    }

    @PermitAll
    @PostMapping
    public ResponseEntity<String> createAlert(@Valid @RequestBody SearchAlertCreateDto dto) {
        LOGGER.info("POST /api/v1/search-alerts");

        if (searchAlertService.alertIsDuplicate(dto)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("You have already saved this search.");
        }

        searchAlertService.createAlert(dto);
        return ResponseEntity.ok().build();
    }

    @PermitAll
    @GetMapping
    public ResponseEntity<List<SearchAlertDetailDto>> getUserAlerts() {
        LOGGER.info("GET /api/v1/search-alerts");
        return ResponseEntity.ok(searchAlertService.getUserAlerts());
    }

    @PermitAll
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        LOGGER.info("DELETE /api/v1/search-alerts/{}", id);
        searchAlertService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }

    @PermitAll
    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateAlertStatus(@PathVariable Long id, @RequestBody boolean active) {
        LOGGER.info("PATCH /api/v1/search-alerts/{}", id);
        searchAlertService.updateAlertStatus(id, active);
        return ResponseEntity.ok().build();
    }

    @PermitAll
    @PatchMapping("/{searchAlertId}/reset-count")
    public ResponseEntity<Void> resetAlertCount(@PathVariable Long searchAlertId) {
        LOGGER.info("POST /api/v1/search-alerts/{}/reset", searchAlertId);
        searchAlertService.resetAlertCount(searchAlertId);
        return ResponseEntity.ok().build();
    }
}
