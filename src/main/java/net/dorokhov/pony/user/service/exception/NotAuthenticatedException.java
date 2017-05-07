package net.dorokhov.pony.user.service.exception;

public final class NotAuthenticatedException extends Exception {

    public NotAuthenticatedException() {
        super("User is not authenticated.");
    }
}
