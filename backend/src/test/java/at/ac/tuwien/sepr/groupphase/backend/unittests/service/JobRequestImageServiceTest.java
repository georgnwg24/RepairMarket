package at.ac.tuwien.sepr.groupphase.backend.unittests.service;

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
import at.ac.tuwien.sepr.groupphase.backend.service.impl.JobRequestImageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobRequestImageServiceTest {

    @Mock
    private JobRequestImageRepository imageRepository;

    @Mock
    private JobRequestRepository jobRequestRepository;

    @Mock
    private JobRequestImageMapper imageMapper;

    @InjectMocks
    private JobRequestImageServiceImpl imageService;

    private static final Long JOB_REQUEST_ID = 1L;
    private static final Long IMAGE_ID = 1L;
    private static final String VALID_IMAGE_TYPE = "image/jpeg";
    private static final byte[] IMAGE_DATA = "image-data".getBytes();

    private JobRequest jobRequest;
    private JobRequestImage image;
    private MultipartFile validFile;
    private MultipartFile invalidFile;

    @BeforeEach
    void setUp() {
        jobRequest = new JobRequest();
        jobRequest.setId(JOB_REQUEST_ID);

        image = JobRequestImage.builder()
            .id(IMAGE_ID)
            .jobRequest(jobRequest)
            .imageType(VALID_IMAGE_TYPE)
            .displayPosition(1)
            .build();

        validFile = new MockMultipartFile(
            "image", "test.jpg", VALID_IMAGE_TYPE, IMAGE_DATA
        );

        invalidFile = new MockMultipartFile(
            "image", "test.txt", "text/plain", "invalid".getBytes()
        );
    }

    @Test
    void createImage_withValidFile_shouldCreateAndReturnDto() {
        // Setup
        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));
        when(imageRepository.countByJobRequestId(JOB_REQUEST_ID)).thenReturn(3);
        when(imageRepository.save(any(JobRequestImage.class))).thenReturn(image);
        when(imageMapper.toDetailDto(image)).thenReturn(new JobRequestImageDetailDto());

        // Execute
        JobRequestImageDetailDto result = imageService.createImage(JOB_REQUEST_ID, validFile, 2);

        // Verify
        assertNotNull(result);
        verify(imageRepository).save(any(JobRequestImage.class));
        verify(imageRepository).findByJobRequestIdOrderByDisplayPositionAsc(JOB_REQUEST_ID);
        verify(imageMapper).toDetailDto(image);
    }

    @Test
    void createImage_whenJobRequestNotFound_shouldThrowNotFoundException() {
        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            imageService.createImage(JOB_REQUEST_ID, validFile, 1)
        );
    }

    @Test
    void createImage_whenImageLimitExceeded_shouldThrowException() {
        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));
        when(imageRepository.countByJobRequestId(JOB_REQUEST_ID)).thenReturn(5);

        assertThrows(ImageLimitExceededException.class, () ->
            imageService.createImage(JOB_REQUEST_ID, validFile, 1)
        );
    }

    @Test
    void createImage_withInvalidFileType_shouldThrowValidationException() {
        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));
        when(imageRepository.countByJobRequestId(JOB_REQUEST_ID)).thenReturn(3);

        assertThrows(ImageValidationException.class, () ->
            imageService.createImage(JOB_REQUEST_ID, invalidFile, 1)
        );
    }

    @Test
    void createImage_withFileReadError_shouldThrowValidationException() throws IOException {
        MultipartFile corruptFile = mock(MultipartFile.class);
        when(corruptFile.getContentType()).thenReturn(VALID_IMAGE_TYPE);
        when(corruptFile.getSize()).thenReturn(1024L);
        when(corruptFile.getBytes()).thenThrow(new IOException("Read error"));

        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));
        when(imageRepository.countByJobRequestId(JOB_REQUEST_ID)).thenReturn(3);

        assertThrows(ImageValidationException.class, () ->
            imageService.createImage(JOB_REQUEST_ID, corruptFile, 1)
        );
    }

    @Test
    void getAllImages_withExistingRequest_shouldReturnDtoList() {
        // Setup
        when(jobRequestRepository.existsById(JOB_REQUEST_ID)).thenReturn(true);
        when(imageRepository.findByJobRequestIdOrderByDisplayPositionAsc(JOB_REQUEST_ID))
            .thenReturn(List.of(image));
        when(imageMapper.toDetailDto(image)).thenReturn(new JobRequestImageDetailDto());

        // Execute
        List<JobRequestImageDetailDto> result = imageService.getAllImages(JOB_REQUEST_ID);

        // Verify
        assertEquals(1, result.size());
        verify(imageMapper).toDetailDto(image);
    }

    @Test
    void getAllImages_whenJobRequestNotFound_shouldThrowNotFoundException() {
        when(jobRequestRepository.existsById(JOB_REQUEST_ID)).thenReturn(false);

        assertThrows(NotFoundException.class, () ->
            imageService.getAllImages(JOB_REQUEST_ID)
        );
    }

    @Test
    void getImageData_withValidIds_shouldReturnDataDto() {
        // Setup
        when(imageRepository.findByJobRequestIdAndId(JOB_REQUEST_ID, IMAGE_ID))
            .thenReturn(Optional.of(image));
        image.setImage(IMAGE_DATA);

        // Execute
        JobRequestImageDataDto result = imageService.getImageData(JOB_REQUEST_ID, IMAGE_ID);

        // Verify
        assertNotNull(result);
        assertEquals(IMAGE_DATA, result.getImageData());
        assertEquals(VALID_IMAGE_TYPE, result.getContentType());
    }

    @Test
    void getImageData_whenImageNotFound_shouldThrowNotFoundException() {
        when(imageRepository.findByJobRequestIdAndId(JOB_REQUEST_ID, IMAGE_ID))
            .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            imageService.getImageData(JOB_REQUEST_ID, IMAGE_ID)
        );
    }

    @Test
    void deleteImage_withValidIds_shouldDelete() {
        when(imageRepository.existsByJobRequestIdAndId(JOB_REQUEST_ID, IMAGE_ID))
            .thenReturn(true);

        assertDoesNotThrow(() ->
            imageService.deleteImage(JOB_REQUEST_ID, IMAGE_ID)
        );

        verify(imageRepository).deleteById(IMAGE_ID);
    }

    @Test
    void deleteImage_whenImageNotFound_shouldThrowNotFoundException() {
        when(imageRepository.existsByJobRequestIdAndId(JOB_REQUEST_ID, IMAGE_ID))
            .thenReturn(false);

        assertThrows(NotFoundException.class, () ->
            imageService.deleteImage(JOB_REQUEST_ID, IMAGE_ID)
        );
    }

    @Test
    void adjustImagePositions_shouldShiftPositionsCorrectly() {
        // Setup - create existing images
        JobRequestImage image1 = new JobRequestImage();
        image1.setId(1L);
        image1.setDisplayPosition(1);

        JobRequestImage image2 = new JobRequestImage();
        image2.setId(2L);
        image2.setDisplayPosition(2);

        JobRequestImage image3 = new JobRequestImage();
        image3.setId(3L);
        image3.setDisplayPosition(3);

        List<JobRequestImage> existingImages = List.of(image1, image2, image3);

        when(jobRequestRepository.findById(JOB_REQUEST_ID)).thenReturn(Optional.of(jobRequest));
        when(imageRepository.countByJobRequestId(JOB_REQUEST_ID)).thenReturn(3);
        when(imageRepository.findByJobRequestIdOrderByDisplayPositionAsc(JOB_REQUEST_ID))
            .thenReturn(existingImages);
        when(imageRepository.save(any(JobRequestImage.class))).thenAnswer(invocation -> {
            JobRequestImage newImage = invocation.getArgument(0);
            newImage.setId(4L);
            return newImage;
        });
        when(imageMapper.toDetailDto(any())).thenReturn(new JobRequestImageDetailDto());

        // Execute - insert at position 2
        imageService.createImage(JOB_REQUEST_ID, validFile, 2);

        // Verify positions were incremented
        assertEquals(1, image1.getDisplayPosition());
        assertEquals(3, image2.getDisplayPosition()); // Shifted from 2 to 3
        assertEquals(4, image3.getDisplayPosition()); // Shifted from 3 to 4
        verify(imageRepository).saveAll(existingImages);
    }
}