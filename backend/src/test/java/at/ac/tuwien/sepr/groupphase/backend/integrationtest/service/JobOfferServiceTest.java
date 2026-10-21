package at.ac.tuwien.sepr.groupphase.backend.integrationtest.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.joboffer.JobOfferDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.OfferAlreadyExistsException;
import at.ac.tuwien.sepr.groupphase.backend.service.JobOfferService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles({"test", "datagen"})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Transactional
public class JobOfferServiceTest {
    @Autowired
    private JobOfferService jobOfferService;

    @BeforeEach
    void setupSecurityContext() {
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken("ethan", "12345678");
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createJobOffer_whenValid_shouldReturnDetailDto() throws Exception {
        JobOfferCreateDto createDto = new JobOfferCreateDto(123.45f, "Looking forward");
        JobOfferDetailDto detail = jobOfferService.createJobOffer(-1108L, createDto);

        assertNotNull(detail);
        assertEquals(-1108L, detail.getJobRequestId());
        assertEquals(-111L, detail.getWorkerId());
        assertEquals(123.45f, detail.getPrice());
        assertEquals("Looking forward", detail.getComment());
        assertNotNull(detail.getCreatedAt());
    }

    @Test
    void createJobOffer_whenDuplicate_shouldThrowOfferAlreadyExistsException() throws Exception {
        JobOfferCreateDto createDto = new JobOfferCreateDto(50f, null);
        jobOfferService.createJobOffer(-1108L, createDto);
        assertThrows(OfferAlreadyExistsException.class, () ->
            jobOfferService.createJobOffer(-1108L, createDto)
        );
    }

    @Test
    void createJobOffer_whenRequestNotExist_shouldThrowEntityNotFoundException() {
        JobOfferCreateDto createDto = new JobOfferCreateDto(50f, null);
        assertThrows(EntityNotFoundException.class, () ->
            jobOfferService.createJobOffer(9999L, createDto)
        );
    }

    @Test
    void createJobOffer_whenNotWorker_shouldThrowAccessDeniedException() {
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken("vince", "12345678");
        SecurityContextHolder.getContext().setAuthentication(auth);
        JobOfferCreateDto createDto = new JobOfferCreateDto(50f, null);
        assertThrows(AccessDeniedException.class, () ->
            jobOfferService.createJobOffer(-1108L, createDto)
        );
    }

    @Test
    void createJobOffer_whenInvalidDto_shouldThrowConstraintViolationException() {
        JobOfferCreateDto createDto = new JobOfferCreateDto(-10f, null);
        assertThrows(ConstraintViolationException.class, () ->
            jobOfferService.createJobOffer(-1108L, createDto)
        );
    }
}
