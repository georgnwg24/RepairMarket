package at.ac.tuwien.sepr.groupphase.backend.exception;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super("User already exists: " + message);
    }
}
