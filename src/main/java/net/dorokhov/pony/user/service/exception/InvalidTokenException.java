package net.dorokhov.pony.user.service.exception;

public final class InvalidTokenException extends Exception {

    public InvalidTokenException() {
        super("User token is invalid.");
    }
}
