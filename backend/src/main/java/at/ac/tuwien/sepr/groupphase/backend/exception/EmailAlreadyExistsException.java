package at.ac.tuwien.sepr.groupphase.backend.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super("Email already exists: " + message);
    }
}
