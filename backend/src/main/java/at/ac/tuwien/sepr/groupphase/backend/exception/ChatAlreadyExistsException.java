package at.ac.tuwien.sepr.groupphase.backend.exception;

public class ChatAlreadyExistsException extends RuntimeException {

    public ChatAlreadyExistsException(String message) {
        super("Chat already exists: " + message);
    }
}
