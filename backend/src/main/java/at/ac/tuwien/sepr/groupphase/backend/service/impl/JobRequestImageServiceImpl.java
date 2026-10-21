package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequestimage.JobRequestImageDataDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequestimage.JobRequestImageDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.JobRequestImageMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequestImage;
import at.ac.tuwien.sepr.groupphase.backend.exception.ImageLimitExceededException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ImageValidationException;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestImageRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.JobRequestRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.JobRequestImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class JobRequestImageServiceImpl implements JobRequestImageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final JobRequestImageRepository imageRepository;
    private final JobRequestRepository jobRequestRepository;
    private final JobRequestImageMapper imageMapper;

    @Autowired
    public JobRequestImageServiceImpl(JobRequestImageRepository imageRepository,
                                      JobRequestRepository jobRequestRepository,
                                      JobRequestImageMapper imageMapper) {
        this.imageRepository = imageRepository;
        this.jobRequestRepository = jobRequestRepository;
        this.imageMapper = imageMapper;
    }

    @Override
    public JobRequestImageDetailDto createImage(Long jobRequestId, MultipartFile file, int displayPosition) throws NotFoundException, ImageValidationException {

        // Get parent job request
        JobRequest jobRequest = jobRequestRepository.findById(jobRequestId)
            .orElseThrow(() -> new NotFoundException("Job request not found"));

        // Validate image count
        int currentImageCount = imageRepository.countByJobRequestId(jobRequestId);
        if (currentImageCount >= 5) {
            throw new ImageLimitExceededException("Maximum 5 images allowed per job request");
        }

        // Validate file and crate the new image
        JobRequestImage image = getJobRequestImage(file, displayPosition, jobRequest);

        // Adjust positions of existing images
        adjustImagePositions(jobRequestId, displayPosition);

        JobRequestImage savedImage = imageRepository.save(image);
        return imageMapper.toDetailDto(savedImage);
    }

    private JobRequestImage getJobRequestImage(MultipartFile file, int displayPosition, JobRequest jobRequest) {
        String contentType = file.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ImageValidationException("Only JPEG/PNG images allowed");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ImageValidationException("Max file size exceeded (5MB)");
        }

        // Create and save image
        JobRequestImage image = new JobRequestImage();
        image.setJobRequest(jobRequest);
        image.setImageType(contentType);
        image.setDisplayPosition(displayPosition);
        try {
            image.setImage(file.getBytes());
        } catch (IOException e) {
            throw new ImageValidationException("Failed to read image data");
        }
        return image;
    }

    private void adjustImagePositions(Long jobRequestId, int newPosition) {
        List<JobRequestImage> images = imageRepository.findByJobRequestIdOrderByDisplayPositionAsc(jobRequestId);

        images.stream()
            .filter(img -> img.getDisplayPosition() >= newPosition)
            .forEach(img -> img.setDisplayPosition(img.getDisplayPosition() + 1));

        imageRepository.saveAll(images);
    }

    @Override
    public List<JobRequestImageDetailDto> getAllImages(Long jobRequestId) throws NotFoundException {
        if (!jobRequestRepository.existsById(jobRequestId)) {
            throw new NotFoundException("Job request not found");
        }

        return imageRepository.findByJobRequestIdOrderByDisplayPositionAsc(jobRequestId)
            .stream()
            .map(imageMapper::toDetailDto)
            .toList();
    }

    @Override
    public JobRequestImageDataDto getImageData(Long jobRequestId, Long imageId) throws NotFoundException {
        JobRequestImage image = imageRepository.findByJobRequestIdAndId(jobRequestId, imageId)
            .orElseThrow(() -> new NotFoundException("Image not found"));

        return new JobRequestImageDataDto(
            image.getImage(),
            image.getImageType()
        );
    }

    @Override
    public void deleteImage(Long jobRequestId, Long imageId) throws NotFoundException {
        if (!imageRepository.existsByJobRequestIdAndId(jobRequestId, imageId)) {
            throw new NotFoundException("Image not found");
        }
        imageRepository.deleteById(imageId);
    }
}
