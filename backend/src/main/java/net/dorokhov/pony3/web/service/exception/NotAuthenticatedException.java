package net.dorokhov.pony3.web.service.exception;

public final class NotAuthenticatedException extends RuntimeException {
    public NotAuthenticatedException() {
        super("User is not authenticated.");
    }
}
