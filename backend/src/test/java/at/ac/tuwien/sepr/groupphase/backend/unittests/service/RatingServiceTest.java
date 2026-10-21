package at.ac.tuwien.sepr.groupphase.backend.unittests.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.rating.RatingDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.rating.RatingStatsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.RatingMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobOffer;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.entity.Rating;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobOfferRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.RatingRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.RatingServiceImpl;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.AccessDeniedException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;
    @Mock
    private UserService userService;
    @Mock
    private RatingMapper ratingMapper;
    @Mock
    private JobRequestRepository jobRequestRepository;
    @Mock
    private JobOfferRepository jobOfferRepository;
    @Mock
    private Validator validator;

    @InjectMocks
    private RatingServiceImpl ratingService;

    private ApplicationUser worker;
    private ApplicationUser customer;
    private RatingDto ratingDto;
    private JobRequest jobRequest;
    private Rating rating;

    @BeforeEach
    void setUp() {
        worker = ApplicationUser.builder()
            .id(1L)
            .username("james")
            .role(Role.WORKER)
            .build();

        customer = ApplicationUser.builder()
            .id(2L)
            .username("jane")
            .role(Role.CUSTOMER)
            .build();

        jobRequest = JobRequest.builder()
            .id(1L)
            .customer(customer)
            .build();

        ratingDto = RatingDto.builder()
            .stars(5)
            .comment("Great job!")
            .build();

        rating = Rating.builder()
            .id(1L)
            .fromUser(worker)
            .toUser(customer)
            .jobRequest(jobRequest)
            .stars(ratingDto.getStars())
            .comment(ratingDto.getComment())
            .build();
    }

    @Test
    void getRatingByRequestId_whenRatingExists_shouldReturnDto() {
        when(userService.getCurrentUser()).thenReturn(worker);
        when(ratingRepository.findByFromUserIdAndJobRequestId(worker.getId(), jobRequest.getId())).thenReturn(Optional.of(rating));
        when(ratingMapper.ratingToDto(rating)).thenReturn(ratingDto);

        RatingDto result = ratingService.getRatingByRequestId(jobRequest.getId());

        assertNotNull(result);
        assertEquals(ratingDto, result);
    }

    @Test
    void getRatingByRequestId_whenRatingDoesNotExist_shouldReturnNull() {
        when(userService.getCurrentUser()).thenReturn(worker);
        when(ratingRepository.findByFromUserIdAndJobRequestId(worker.getId(), 1L)).thenReturn(Optional.empty());

        RatingDto result = ratingService.getRatingByRequestId(1L);

        assertNull(result);
    }

    @Test
    void createRating_byWorker_shouldSucceedAndSetCorrectUsers() throws AccessDeniedException {

        when(userService.getCurrentUser()).thenReturn(worker);
        when(ratingRepository.findByFromUserIdAndJobRequestId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(jobRequestRepository.findById(anyLong())).thenReturn(Optional.of(jobRequest));
        when(ratingMapper.dtoToRating(any(RatingDto.class))).thenReturn(new Rating());


        ratingService.createRating(jobRequest.getId(), ratingDto);

        ArgumentCaptor<Rating> captor = ArgumentCaptor.forClass(Rating.class);
        verify(ratingRepository).save(captor.capture());
        Rating savedRating = captor.getValue();

        assertEquals(worker, savedRating.getFromUser());
        assertEquals(customer, savedRating.getToUser());
        assertEquals(jobRequest, savedRating.getJobRequest());
    }

    @Test
    void createRating_byCustomer_shouldSucceedAndSetCorrectUsers() throws AccessDeniedException {

        JobOffer completedOffer = new JobOffer();
        completedOffer.setWorker(worker);

        when(userService.getCurrentUser()).thenReturn(customer);
        when(ratingRepository.findByFromUserIdAndJobRequestId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(jobRequestRepository.findById(anyLong())).thenReturn(Optional.of(jobRequest));
        when(jobOfferRepository.findCompletedOfferByJobRequestId(anyLong())).thenReturn(Optional.of(completedOffer));
        when(ratingMapper.dtoToRating(any(RatingDto.class))).thenReturn(new Rating());

        ratingService.createRating(jobRequest.getId(), ratingDto);

        ArgumentCaptor<Rating> captor = ArgumentCaptor.forClass(Rating.class);
        verify(ratingRepository).save(captor.capture());
        Rating savedRating = captor.getValue();

        assertEquals(customer, savedRating.getFromUser());
        assertEquals(worker, savedRating.getToUser());
        assertEquals(jobRequest, savedRating.getJobRequest());
    }


    @Test
    void createRating_whenAlreadyExists_shouldThrowIllegalStateException() {
        when(userService.getCurrentUser()).thenReturn(worker);
        when(ratingRepository.findByFromUserIdAndJobRequestId(1L, 1L)).thenReturn(Optional.of(rating));

        assertThrows(IllegalStateException.class, () -> ratingService.createRating(1L, new RatingDto()));
    }

    @Test
    void createRating_whenJobRequestNotFound_shouldThrowEntityNotFoundException() {
        when(userService.getCurrentUser()).thenReturn(worker);
        when(ratingRepository.findByFromUserIdAndJobRequestId(worker.getId(), 99L)).thenReturn(Optional.empty());
        when(jobRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> ratingService.createRating(99L, ratingDto));
    }

    @Test
    void createRating_byCustomerWhenNoCompletedOffer_shouldThrowNotFoundException() {
        when(userService.getCurrentUser()).thenReturn(customer);
        when(ratingRepository.findByFromUserIdAndJobRequestId(customer.getId(), jobRequest.getId())).thenReturn(Optional.empty());
        when(jobRequestRepository.findById(jobRequest.getId())).thenReturn(Optional.of(jobRequest));
        when(jobOfferRepository.findCompletedOfferByJobRequestId(jobRequest.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ratingService.createRating(jobRequest.getId(), ratingDto));
    }


    @Test
    void updateRating_withValidData_shouldUpdateAndSave() {
        ratingDto.setStars(4);
        ratingDto.setComment("Updated comment");
        when(userService.getCurrentUser()).thenReturn(worker);
        when(ratingRepository.findByFromUserIdAndJobRequestId(worker.getId(), 1L)).thenReturn(Optional.of(rating));
        when(validator.validate(ratingDto)).thenReturn(Collections.emptySet());

        ratingService.updateRating(1L, ratingDto);

        assertEquals(4, rating.getStars());
        assertEquals("Updated comment", rating.getComment());
        verify(ratingRepository).save(rating);
    }

    @Test
    void updateRating_whenRatingNotFound_shouldThrowEntityNotFoundException() {
        when(userService.getCurrentUser()).thenReturn(worker);
        when(ratingRepository.findByFromUserIdAndJobRequestId(worker.getId(), 1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> ratingService.updateRating(1L, new RatingDto()));
    }

    @Test
    void updateRating_withInvalidDto_shouldThrowConstraintViolationException() {
        Set<ConstraintViolation<RatingDto>> violations = new HashSet<>();
        violations.add(mock(ConstraintViolation.class));

        when(userService.getCurrentUser()).thenReturn(worker);
        when(ratingRepository.findByFromUserIdAndJobRequestId(worker.getId(), jobRequest.getId())).thenReturn(Optional.of(rating));
        when(validator.validate(ratingDto)).thenReturn(violations);

        assertThrows(ConstraintViolationException.class, () -> ratingService.updateRating(jobRequest.getId(), ratingDto));
    }

    @Test
    void getLatestRatings_shouldReturnMappedDtosOrEmptyList() {

        when(ratingRepository.findTop10ByToUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(rating));
        when(ratingMapper.ratingToDto(any(Rating.class))).thenReturn(new RatingDto());

        List<RatingDto> resultWithRatings = ratingService.getLatestRatings(1L);
        assertEquals(1, resultWithRatings.size());

        when(ratingRepository.findTop10ByToUserIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());
        List<RatingDto> resultWithoutRatings = ratingService.getLatestRatings(1L);
        assertTrue(resultWithoutRatings.isEmpty());
    }

    @Test
    void getRatingStats_shouldCalculateCorrectlyForAnyNumberOfRatings() {
        when(ratingRepository.findAllByToUserId(1L)).thenReturn(Collections.emptyList());
        RatingStatsDto zeroStats = ratingService.getRatingStats(1L);
        assertEquals(0.0f, zeroStats.getAverage());
        assertEquals(0, zeroStats.getCount());

        when(ratingRepository.findAllByToUserId(1L)).thenReturn(List.of(rating));
        RatingStatsDto singleStats = ratingService.getRatingStats(1L);
        assertEquals(5.0f, singleStats.getAverage());
        assertEquals(1, singleStats.getCount());

        Rating rating1 = Rating.builder().stars(3).build();
        Rating rating2 = Rating.builder().stars(5).build();
        when(ratingRepository.findAllByToUserId(1L)).thenReturn(List.of(rating1, rating2));
        RatingStatsDto multiStats = ratingService.getRatingStats(1L);
        assertEquals(4.0f, multiStats.getAverage());
        assertEquals(2, multiStats.getCount());
    }
}