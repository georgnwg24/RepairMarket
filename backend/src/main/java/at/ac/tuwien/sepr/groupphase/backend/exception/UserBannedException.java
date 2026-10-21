package at.ac.tuwien.sepr.groupphase.backend.exception;

public class UserBannedException extends RuntimeException {
    public UserBannedException(String message) {
        super(message);
    }
}
