package net.dorokhov.pony.security.service.exception;

public final class NotAuthenticatedException extends RuntimeException {

    public NotAuthenticatedException() {
        super("User is not authenticated.");
    }
}
