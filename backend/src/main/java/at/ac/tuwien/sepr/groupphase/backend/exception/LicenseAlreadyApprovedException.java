package at.ac.tuwien.sepr.groupphase.backend.exception;

public class LicenseAlreadyApprovedException extends RuntimeException {
    public LicenseAlreadyApprovedException() {
        super("This License has already been approved");
    }
}