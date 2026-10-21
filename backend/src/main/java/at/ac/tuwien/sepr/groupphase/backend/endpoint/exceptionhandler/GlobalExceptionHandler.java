package at.ac.tuwien.sepr.groupphase.backend.endpoint.exceptionhandler;

import at.ac.tuwien.sepr.groupphase.backend.exception.ActiveJobRequestsExistException;
import at.ac.tuwien.sepr.groupphase.backend.exception.AlreadyReportedException;
import at.ac.tuwien.sepr.groupphase.backend.exception.EmailAlreadyExistsException;
import at.ac.tuwien.sepr.groupphase.backend.exception.FileSizeLimitExceededException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ImageInvalidContentTypeException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ImageLimitExceededException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ImageUploadException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ImageValidationException;
import at.ac.tuwien.sepr.groupphase.backend.exception.LicenseAlreadyApprovedException;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.UserAlreadyExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

import java.nio.file.AccessDeniedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Register all your Java exceptions here to map them into meaningful HTTP exceptions.
 * If you have special cases which are only important for specific endpoints, use ResponseStatusExceptions
 * https://www.baeldung.com/exception-handling-for-rest-with-spring#responsestatusexception
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Use the @ExceptionHandler annotation to write handler for custom exceptions.
     */
    @ExceptionHandler(value = {NotFoundException.class})
    protected ResponseEntity<Object> handleNotFound(RuntimeException ex, WebRequest request) {
        LOGGER.warn(ex.getMessage());
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    /**
     * Override methods from ResponseEntityExceptionHandler to send a customized HTTP response for a know exception
     * from e.g. Spring
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        //Get all errors
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(err -> err.getField() + " " + err.getDefaultMessage())
            .collect(Collectors.toList());
        body.put("Validation errors", errors);

        return new ResponseEntity<>(body.toString(), headers, status);

    }

    @ExceptionHandler(value = {ConstraintViolationException.class})
    protected ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        LOGGER.warn("Validation failed: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Validation errors", ex.getConstraintViolations()
            .stream()
            .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
            .collect(Collectors.toList()));

        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    protected ResponseEntity<Object> handleEmailExists(BadCredentialsException ex, WebRequest request) {
        LOGGER.warn("Bad Credentials: {}", ex.getMessage());
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    protected ResponseEntity<Object> handleEmailExists(EmailAlreadyExistsException ex, WebRequest request) {
        LOGGER.warn("Email already exists: {}", ex.getMessage());
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    protected ResponseEntity<Object> handleUserExists(UserAlreadyExistsException ex, WebRequest request) {
        LOGGER.warn("Username already exists: {}", ex.getMessage());
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(ImageValidationException.class)
    protected ResponseEntity<Object> handleImageValidation(ImageValidationException ex, WebRequest request) {
        LOGGER.warn("Image validation failed: {}", ex.getMessage());
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(ImageLimitExceededException.class)
    protected ResponseEntity<Object> handleImageLimitExceeded(ImageLimitExceededException ex, WebRequest request) {
        LOGGER.warn("Image limit exceeded: {}", ex.getMessage());
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(ActiveJobRequestsExistException.class)
    protected ResponseEntity<Object> handleActiveJobRequests(ActiveJobRequestsExistException ex, WebRequest request) {
        LOGGER.warn("Attempt to delete property with active job requests: {}", ex.getMessage());
        Map<String, String> body = Map.of("message", ex.getMessage());
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(IOException.class)
    protected ResponseEntity<Object> handleioException(IOException ex, WebRequest request) {
        LOGGER.error("I/O error occurred: {}", ex.getMessage(), ex);

        Map<String, String> body = Map.of("message", "Failed to process the uploaded file. Please try again.");
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<Object> handleUnauthorized(AccessDeniedException ex, WebRequest request) {
        LOGGER.warn("No permission to execute: {}", ex.getMessage());
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    protected ResponseEntity<Object> handleEntityNotFoundExists(EntityNotFoundException ex, WebRequest request) {
        LOGGER.warn("{}", ex.getMessage());
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    protected ResponseEntity<Object> handleUnsupportedOperationException(UnsupportedOperationException ex, WebRequest request) {
        LOGGER.warn("{}", ex.getMessage());
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.METHOD_NOT_ALLOWED, request);
    }

    @ExceptionHandler(IllegalStateException.class)
    protected ResponseEntity<Object> handleIllegalState(IllegalStateException ex, WebRequest request) {
        LOGGER.warn("Wrong state for operation: {}", ex.getMessage());
        Map<String, String> body = Map.of("message", ex.getMessage());
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(LicenseAlreadyApprovedException.class)
    protected ResponseEntity<Object> handleLicenseApproved(LicenseAlreadyApprovedException ex, WebRequest request) {
        LOGGER.warn("License already approved: {}", ex.getMessage());
        Map<String, String> body = Map.of("message", ex.getMessage());
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(ImageUploadException.class)
    protected ResponseEntity<Object> handleFileUploadException(ImageUploadException ex, WebRequest request) {
        LOGGER.warn("File upload failed: {}", ex.getMessage());
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.UNPROCESSABLE_ENTITY, request);
    }

    @ExceptionHandler(ImageInvalidContentTypeException.class)
    protected ResponseEntity<Object> handleFileUploadException(ImageInvalidContentTypeException ex, WebRequest request) {
        LOGGER.warn("Invalid file content-type: {}", ex.getMessage());
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.UNSUPPORTED_MEDIA_TYPE, request);
    }

    @ExceptionHandler(FileSizeLimitExceededException.class)
    protected ResponseEntity<Object> handleFileUploadException(FileSizeLimitExceededException ex, WebRequest request) {
        LOGGER.warn("File size limit exceeded: {}", ex.getMessage());
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.PAYLOAD_TOO_LARGE, request);
    }

    @ExceptionHandler(AlreadyReportedException.class)
    protected ResponseEntity<Object> handleAlreadyReportedException(AlreadyReportedException ex, WebRequest request) {
        LOGGER.warn("Already reported: {}", ex.getMessage());
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.CONFLICT, request);
    }
}
