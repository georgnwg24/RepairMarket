package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequestimage.JobRequestImageDataDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.jobrequestimage.JobRequestImageDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ImageValidationException;
import at.ac.tuwien.sepr.groupphase.backend.service.JobRequestImageService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Endpoint for managing images associated with job requests.
 */
@RestController
@RequestMapping(path = JobRequestEndpoint.BASE_PATH + "/{jobRequestId}/images")
public class JobRequestImageEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final JobRequestImageService imageService;
    private final UserService userService;

    @Autowired
    public JobRequestImageEndpoint(JobRequestImageService imageService, UserService userService) {
        this.imageService = imageService;
        this.userService = userService;
    }

    /**
     * Uploads a new image for a job request.
     *
     * @param jobRequestId    ID of the job request to associate the image with
     * @param file            Image file to upload (JPEG or PNG, max 5MB)
     * @return Created image metadata with HTTP 201 status
     * @throws NotFoundException        If the job request doesn't exist
     * @throws ImageValidationException If job request already has more than 5 images, if the file is not JPEG/PNG or >5MB
     */
    @PermitAll
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<JobRequestImageDetailDto> uploadImage(
        @PathVariable Long jobRequestId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(defaultValue = "0") int displayPosition
    ) {
        LOGGER.info("Uploading image for job request {}", jobRequestId);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(imageService.createImage(jobRequestId, file, displayPosition));
    }

    /**
     * Retrieves all images associated with a job request.
     *
     * @param jobRequestId ID of the job request
     * @return List of image metadata ordered by display position
     * @throws NotFoundException If the job request doesn't exist
     */
    @PermitAll
    @GetMapping
    public List<JobRequestImageDetailDto> getAllImages(@PathVariable Long jobRequestId) {
        LOGGER.info("Getting all images for job request {}", jobRequestId);
        return imageService.getAllImages(jobRequestId);
    }

    /**
     * Retrieves a single image file.
     *
     * @param jobRequestId ID of the job request
     * @param imageId      ID of the image to retrieve
     * @return Image bytes with appropriate content-type header
     * @throws NotFoundException        If the image or job request doesn't exist
     */
    @PermitAll
    @GetMapping("/{imageId}")
    public ResponseEntity<byte[]> getImage(
        @PathVariable Long jobRequestId,
        @PathVariable Long imageId
    ) {
        LOGGER.info("Getting image {} for job request {}", imageId, jobRequestId);
        JobRequestImageDataDto image = imageService.getImageData(jobRequestId, imageId);
        byte[] imageData = image.getImageData();
        String contentType = image.getContentType();

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(imageData);
    }

    /**
     * Deletes an image from a job request.
     *
     * @param jobRequestId ID of the job request
     * @param imageId      ID of the image to delete
     * @throws NotFoundException If the image or job request doesn't exist
     */
    @PermitAll
    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(
        @PathVariable Long jobRequestId,
        @PathVariable Long imageId
    ) {
        LOGGER.info("Deleting image {} from job request {}", imageId, jobRequestId);
        imageService.deleteImage(jobRequestId, imageId);
    }
}
