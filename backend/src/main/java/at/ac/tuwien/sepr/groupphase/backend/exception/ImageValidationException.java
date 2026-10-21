package at.ac.tuwien.sepr.groupphase.backend.exception;

public class ImageValidationException extends RuntimeException {
    public ImageValidationException(String message) {
        super("During the image validation an error occurred:" + message);
    }
}
