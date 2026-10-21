package at.ac.tuwien.sepr.groupphase.backend.exception;

public class ActiveJobRequestsExistException extends RuntimeException {
    public ActiveJobRequestsExistException() {
        super("This property is currently being used by a JobRequest, please remove it first");
    }
}