package at.ac.tuwien.sepr.groupphase.backend.integrationtest.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequest.JobRequestUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.service.JobRequestService;
import at.ac.tuwien.sepr.groupphase.backend.type.Category;
import at.ac.tuwien.sepr.groupphase.backend.type.JobStatus;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles({"test", "datagen"})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Transactional
public class JobRequestServiceTest {

    @Autowired
    private JobRequestService jobRequestService;

    @BeforeEach
    void setupSecurityContext() {
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken("ned", "12345678");
        SecurityContextHolder.getContext().setAuthentication(auth);
    }


    @Test
    void createJobRequest_whenValid_shouldReturnDetailDto() {
        JobRequestCreateDto createDto = new JobRequestCreateDto();
        createDto.setTitle("Fix my house");
        createDto.setDescription("Roof is leaking");
        createDto.setCategory(Category.CARPENTRY);
        createDto.setDeadline(LocalDate.now().plusDays(10));
        createDto.setStatus(JobStatus.PENDING);
        createDto.setPropertyId(null);

        JobRequestDetailDto detailDto = jobRequestService.create(createDto);

        assertNotNull(detailDto);
        assertEquals("Fix my house", detailDto.getTitle());
        assertEquals("Roof is leaking", detailDto.getDescription());
        assertEquals(Category.CARPENTRY, detailDto.getCategory());
        assertEquals(LocalDate.now().plusDays(10), detailDto.getDeadline());
        assertNotNull(detailDto.getId());
    }

    @Test
    void createJobRequest_withNonExistingProperty_shouldThrowNotFoundException() {
        JobRequestCreateDto createDto = new JobRequestCreateDto();
        createDto.setTitle("Test");
        createDto.setPropertyId(9999L);

        assertThrows(NotFoundException.class, () -> jobRequestService.create(createDto));
    }

    @Test
    void getById_withExistingId_shouldReturnDetailDto() {
        JobRequestCreateDto createDto = new JobRequestCreateDto();
        createDto.setTitle("Test GetById");
        createDto.setDescription("Description");
        createDto.setCategory(Category.FLOORING);
        createDto.setStatus(JobStatus.PENDING);
        createDto.setDeadline(LocalDate.now().plusDays(5));
        JobRequestDetailDto created = jobRequestService.create(createDto);

        JobRequestDetailDto fetched = jobRequestService.getById(created.getId());

        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
    }

    @Test
    void getById_withNonExistingId_shouldThrowNotFoundException() {
        assertThrows(NotFoundException.class, () -> jobRequestService.getById(99999L));
    }

    @Test
    void updateJobRequest_whenValid_shouldUpdateAndReturnUpdateDto() {

        JobRequestCreateDto createDto = new JobRequestCreateDto();
        createDto.setTitle("Before Update");
        createDto.setDescription("Desc");
        createDto.setCategory(Category.ELECTRICAL);
        createDto.setStatus(JobStatus.PENDING);
        createDto.setDeadline(LocalDate.now().plusDays(3));

        JobRequestDetailDto created = jobRequestService.create(createDto);

        JobRequestUpdateDto updateDto = new JobRequestUpdateDto();
        updateDto.setId(created.getId());
        updateDto.setTitle("After Update");
        updateDto.setDescription("Updated Desc");
        updateDto.setCategory(Category.MOVING);
        updateDto.setDeadline(LocalDate.now().plusDays(7));
        updateDto.setPropertyId(null);

        JobRequestUpdateDto updated = jobRequestService.update(updateDto);

        assertNotNull(updated);
        assertEquals("After Update", updated.getTitle());
        assertEquals("Updated Desc", updated.getDescription());
    }

    @Test
    void listJobRequests_shouldReturnNonEmptyList() {
        var list = jobRequestService.listJobRequests();
        assertNotNull(list);
        assertFalse(list.isEmpty());
    }

    @Test
    void deleteById_whenOwner_shouldDelete() {
        JobRequestCreateDto createDto = new JobRequestCreateDto();
        createDto.setTitle("To be deleted");
        createDto.setDescription("Delete me");
        createDto.setStatus(JobStatus.PENDING);
        createDto.setCategory(Category.MOVING);
        createDto.setDeadline(LocalDate.now().plusDays(2));

        JobRequestDetailDto created = jobRequestService.create(createDto);

        assertDoesNotThrow(() -> jobRequestService.deleteById(created.getId()));

        assertThrows(NotFoundException.class, () -> jobRequestService.getById(created.getId()));
    }

    @Test
    void findAll_shouldReturnList() {
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken("ethan", "12345678");
        SecurityContextHolder.getContext().setAuthentication(auth);

        var list = jobRequestService.findAllOpen();
        assertNotNull(list);
    }
}
