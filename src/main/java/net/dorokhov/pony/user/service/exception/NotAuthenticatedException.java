package net.dorokhov.pony.user.service.exception;

public class NotAuthenticatedException extends RuntimeException {

    public NotAuthenticatedException() {
        super("User is not authenticated.");
    }
}
