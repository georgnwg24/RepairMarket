package at.ac.tuwien.sepr.groupphase.backend.unittests.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.searchalert.SearchAlertCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.searchalert.SearchAlertDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.SearchAlertMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.SearchAlert;
import at.ac.tuwien.sepr.groupphase.backend.repository.SearchAlertRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.SearchAlertServiceImpl;
import at.ac.tuwien.sepr.groupphase.backend.type.Category;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchAlertServiceTest {

    @Mock
    private SearchAlertRepository searchAlertRepository;
    @Mock
    private UserService userService;
    @Mock
    private SearchAlertMapper searchAlertMapper;

    @InjectMocks
    private SearchAlertServiceImpl searchAlertService;

    private ApplicationUser testUser;
    private SearchAlert testAlert;

    @BeforeEach
    void setUp() {
        testUser = new ApplicationUser();
        testUser.setId(1L);

        testAlert = new SearchAlert();
        testAlert.setId(100L);
        testAlert.setWorker(testUser);
        testAlert.setKeywords("plumber");
        testAlert.setMaxDistance(50);
        testAlert.setCategories(List.of(Category.PLUMBING));
        testAlert.setActive(true);
        testAlert.setCount(0);

        when(userService.getCurrentUser()).thenReturn(testUser);
    }

    @Test
    void createAlert_shouldSaveAlertWithCurrentUser() {
        SearchAlertCreateDto createDto = new SearchAlertCreateDto();
        when(searchAlertMapper.toEntity(createDto)).thenReturn(testAlert);

        searchAlertService.createAlert(createDto);

        ArgumentCaptor<SearchAlert> alertCaptor = ArgumentCaptor.forClass(SearchAlert.class);
        verify(searchAlertRepository).save(alertCaptor.capture());
        assertEquals(testUser, alertCaptor.getValue().getWorker());
    }

    @Test
    void alertIsDuplicate_whenDuplicateExists_returnsTrue() {
        SearchAlertCreateDto dto = new SearchAlertCreateDto("plumber", 50, List.of(String.valueOf(Category.PLUMBING)));
        when(searchAlertRepository.findByWorker(testUser)).thenReturn(List.of(testAlert));
        when(searchAlertMapper.mapCategories(dto.getCategories())).thenReturn(List.of(Category.PLUMBING));

        boolean isDuplicate = searchAlertService.alertIsDuplicate(dto);

        assertTrue(isDuplicate);
    }

    @Test
    void alertIsDuplicate_whenNoDuplicateExists_returnsFalse() {
        SearchAlertCreateDto dto = new SearchAlertCreateDto("electrician", 20, List.of(String.valueOf(Category.ELECTRICAL)));
        when(searchAlertRepository.findByWorker(testUser)).thenReturn(List.of(testAlert));
        when(searchAlertMapper.mapCategories(dto.getCategories())).thenReturn(List.of(Category.ELECTRICAL));

        boolean isDuplicate = searchAlertService.alertIsDuplicate(dto);

        assertFalse(isDuplicate);
    }

    @Test
    void getUserAlerts_returnsListOfDetailDtos() {
        when(searchAlertRepository.findByWorker(testUser)).thenReturn(List.of(testAlert));
        when(searchAlertMapper.toDetailDto(any(SearchAlert.class))).thenReturn(new SearchAlertDetailDto());

        List<SearchAlertDetailDto> alerts = searchAlertService.getUserAlerts();

        assertNotNull(alerts);
        assertEquals(1, alerts.size());
        verify(searchAlertMapper).toDetailDto(testAlert);
    }

    @Test
    void deleteAlert_whenUserIsOwner_deletesAlert() {
        when(searchAlertRepository.findById(testAlert.getId())).thenReturn(Optional.of(testAlert));

        searchAlertService.deleteAlert(testAlert.getId());

        verify(searchAlertRepository).delete(testAlert);
    }

    @Test
    void deleteAlert_whenAlertNotFound_throwsEntityNotFoundException() {
        when(searchAlertRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> searchAlertService.deleteAlert(999L));
    }

    @Test
    void deleteAlert_whenUserIsNotOwner_throwsAccessDeniedException() {
        ApplicationUser anotherUser = new ApplicationUser();
        anotherUser.setId(2L);
        testAlert.setWorker(anotherUser);
        when(searchAlertRepository.findById(testAlert.getId())).thenReturn(Optional.of(testAlert));

        assertThrows(AccessDeniedException.class, () -> searchAlertService.deleteAlert(testAlert.getId()));
    }

    @Test
    void updateAlertStatus_whenUserIsOwner_updatesAndSaves() {
        when(searchAlertRepository.findById(testAlert.getId())).thenReturn(Optional.of(testAlert));

        searchAlertService.updateAlertStatus(testAlert.getId(), false);

        ArgumentCaptor<SearchAlert> alertCaptor = ArgumentCaptor.forClass(SearchAlert.class);
        verify(searchAlertRepository).save(alertCaptor.capture());
        assertFalse(alertCaptor.getValue().isActive());
    }

    @Test
    void resetAlertCount_whenUserIsOwner_resetsAndSaves() {
        testAlert.setCount(5);
        when(searchAlertRepository.findById(testAlert.getId())).thenReturn(Optional.of(testAlert));

        searchAlertService.resetAlertCount(testAlert.getId());

        ArgumentCaptor<SearchAlert> alertCaptor = ArgumentCaptor.forClass(SearchAlert.class);
        verify(searchAlertRepository).save(alertCaptor.capture());
        assertEquals(0, alertCaptor.getValue().getCount());
    }
}