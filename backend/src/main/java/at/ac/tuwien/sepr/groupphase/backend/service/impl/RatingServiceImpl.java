package at.ac.tuwien.sepr.groupphase.backend.service.impl;

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
import at.ac.tuwien.sepr.groupphase.backend.service.RatingService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.type.Role;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RatingServiceImpl implements RatingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final RatingRepository ratingRepository;
    private final UserService userService;
    private final RatingMapper ratingMapper;
    private final JobRequestRepository jobRequestRepository;
    private final JobOfferRepository jobOfferRepository;
    private final Validator validator;

    @Autowired
    public RatingServiceImpl(RatingRepository ratingRepository, UserService userService, RatingMapper ratingMapper, JobRequestRepository jobRequestRepository, JobOfferRepository jobOfferRepository, Validator validator) {
        this.ratingRepository = ratingRepository;
        this.userService = userService;
        this.ratingMapper = ratingMapper;
        this.jobRequestRepository = jobRequestRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.validator = validator;
    }

    @Override
    public RatingDto getRatingByRequestId(Long jobRequestId) {
        ApplicationUser user = userService.getCurrentUser();
        return ratingRepository.findByFromUserIdAndJobRequestId(user.getId(), jobRequestId)
            .map(ratingMapper::ratingToDto)
            .orElse(null);
    }

    @Override
    public void createRating(Long jobRequestId, RatingDto ratingDto) throws AccessDeniedException {
        ApplicationUser currentUser = userService.getCurrentUser();

        if (ratingRepository.findByFromUserIdAndJobRequestId(currentUser.getId(), jobRequestId).isPresent()) {
            throw new IllegalStateException("Rating already exists for this job request by this user.");
        }

        JobRequest jobRequest = jobRequestRepository.findById(jobRequestId)
            .orElseThrow(() -> new EntityNotFoundException("Job request not found"));

        ApplicationUser toUser = jobRequest.getCustomer();
        if (currentUser.getRole() == Role.CUSTOMER) {
            JobOffer jobOffer = jobOfferRepository.findCompletedOfferByJobRequestId(jobRequestId)
                .orElseThrow(() -> new NotFoundException("No completed JobOffer found for this jobrequest"));
            toUser = jobOffer.getWorker();
        }

        Rating rating = ratingMapper.dtoToRating(ratingDto);
        rating.setFromUser(currentUser);
        rating.setToUser(toUser);
        rating.setJobRequest(jobRequest);
        ratingRepository.save(rating);
    }

    @Override
    public void updateRating(Long jobRequestId, RatingDto ratingDto) {
        ApplicationUser currentUser = userService.getCurrentUser();

        Rating rating = ratingRepository.findByFromUserIdAndJobRequestId(currentUser.getId(), jobRequestId)
            .orElseThrow(() -> new EntityNotFoundException("No existing rating to update."));

        Set<ConstraintViolation<RatingDto>> violations = validator.validate(ratingDto);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        rating.setStars(ratingDto.getStars());
        rating.setComment(ratingDto.getComment());

        ratingRepository.save(rating);
    }

    @Override
    public List<RatingDto> getLatestRatings(Long userId) {
        List<Rating> ratings = ratingRepository.findTop10ByToUserIdOrderByCreatedAtDesc(userId);
        return ratings.stream()
            .map(ratingMapper::ratingToDto)
            .collect(Collectors.toList());
    }

    @Override
    public RatingStatsDto getRatingStats(Long userId) {
        List<Rating> ratings = ratingRepository.findAllByToUserId(userId);
        if (ratings.isEmpty()) {
            return new RatingStatsDto(0.0f, 0);
        }

        float avg = (float) ratings.stream().mapToInt(Rating::getStars).average().orElse(0.0f);
        return new RatingStatsDto(avg, ratings.size());
    }


}
