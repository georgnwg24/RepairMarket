package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.searchalert.SearchAlertCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.searchalert.SearchAlertDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.SearchAlertMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.SearchAlert;
import at.ac.tuwien.sepr.groupphase.backend.repository.SearchAlertRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.SearchAlertService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.type.Category;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
public class SearchAlertServiceImpl implements SearchAlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final SearchAlertRepository searchAlertRepository;
    private final UserService userService;
    private final SearchAlertMapper searchAlertMapper;

    @Autowired
    public SearchAlertServiceImpl(SearchAlertRepository repository, UserService userService, SearchAlertMapper mapper) {
        this.searchAlertRepository = repository;
        this.userService = userService;
        this.searchAlertMapper = mapper;
    }

    @Override
    public void createAlert(SearchAlertCreateDto dto) {
        ApplicationUser worker = userService.getCurrentUser();

        SearchAlert searchAlert = searchAlertMapper.toEntity(dto);
        searchAlert.setWorker(worker);
        searchAlertRepository.save(searchAlert);
    }

    @Override
    @Transactional
    public boolean alertIsDuplicate(SearchAlertCreateDto dto) {
        ApplicationUser worker = userService.getCurrentUser();

        List<SearchAlert> existingAlerts = searchAlertRepository.findByWorker(worker);
        List<Category> dtoCategories = searchAlertMapper.mapCategories(dto.getCategories());

        return existingAlerts.stream().anyMatch(alert ->
            Objects.equals(alert.getKeywords(), dto.getKeywords())
                && Objects.equals(alert.getMaxDistance(), dto.getMaxDistance())
                && new HashSet<>(alert.getCategories()).equals(new HashSet<>(dtoCategories))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchAlertDetailDto> getUserAlerts() {
        ApplicationUser worker = userService.getCurrentUser();
        return searchAlertRepository.findByWorker(worker).stream()
            .map(searchAlertMapper::toDetailDto)
            .toList();
    }

    @Override
    public void deleteAlert(Long id) {
        ApplicationUser worker = userService.getCurrentUser();

        SearchAlert alert = searchAlertRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("SearchAlert with id " + id + " not found"));

        if (!alert.getWorker().getId().equals(worker.getId())) {
            throw new AccessDeniedException("You can only delete your own search alerts");
        }

        searchAlertRepository.delete(alert);
    }

    @Override
    public void updateAlertStatus(Long id, boolean active) {
        SearchAlert alert = searchAlertRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("SearchAlert with id " + id + " not found"));

        ApplicationUser worker = userService.getCurrentUser();
        if (!alert.getWorker().getId().equals(worker.getId())) {
            throw new AccessDeniedException("You can only delete your own search alerts");
        }

        alert.setActive(active);
        searchAlertRepository.save(alert);
    }

    @Override
    public void resetAlertCount(Long id) {
        ApplicationUser worker = userService.getCurrentUser();
        SearchAlert alert = searchAlertRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("SearchAlert with id " + id + " not found"));

        if (!alert.getWorker().getId().equals(worker.getId())) {
            throw new AccessDeniedException("You can only reset your own search alerts");
        }

        alert.setCount(0);
        searchAlertRepository.save(alert);
    }
}
