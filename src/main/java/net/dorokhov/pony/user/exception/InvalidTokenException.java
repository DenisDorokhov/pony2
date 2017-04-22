package net.dorokhov.pony.user.exception;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("User token is invalid.");
    }
}
