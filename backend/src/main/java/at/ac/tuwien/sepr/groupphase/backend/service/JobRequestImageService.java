package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequestimage.JobRequestImageDataDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequestimage.JobRequestImageDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ImageValidationException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ImageLimitExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for managing images associated with job requests.
 */
public interface JobRequestImageService {

    /**
     * Uploads a new image to a job request.
     *
     * @param jobRequestId ID of the job request to associate with the image
     * @param file Multipart file containing the image data (JPEG/PNG only)
     * @param displayPosition Position on which the image should be displayed (0 = first)
     * @return DTO containing image metadata
     * @throws NotFoundException if job request doesn't exist
     * @throws ImageValidationException if the file is not JPEG/PNG or >5MB
     * @throws ImageLimitExceededException if job request already has more than 5 images
     */
    JobRequestImageDetailDto createImage(Long jobRequestId, MultipartFile file, int displayPosition)
        throws NotFoundException,
        ImageValidationException,
        ImageLimitExceededException;

    /**
     * Retrieves all images for a job request.
     *
     * @param jobRequestId ID of the job request
     * @return List of image DTOs sorted by display position
     * @throws NotFoundException if job request doesn't exist
     */
    List<JobRequestImageDetailDto> getAllImages(Long jobRequestId)
        throws NotFoundException;

    /**
     * Retrieves raw image data.
     *
     * @param jobRequestId ID of the parent job request
     * @param imageId ID of the image to retrieve
     * @return A JobRequestImageDataDto containing image bytes with content type
     * @throws NotFoundException if image/job request doesn't exist
     */
    JobRequestImageDataDto getImageData(Long jobRequestId, Long imageId)
        throws NotFoundException;

    /**
     * Deletes an image from a job request.
     *
     * @param jobRequestId ID of the parent job request
     * @param imageId ID of the image to delete
     * @throws NotFoundException if image/job request doesn't exist
     */
    void deleteImage(Long jobRequestId, Long imageId)
        throws NotFoundException;
}
