package net.dorokhov.pony.user.service.exception;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("User token is invalid.");
    }
}
