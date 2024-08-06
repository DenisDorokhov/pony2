package net.dorokhov.pony2.web.service.exception;

public final class NotAuthenticatedException extends RuntimeException {
    public NotAuthenticatedException() {
        super("User is not authenticated.");
    }
}
