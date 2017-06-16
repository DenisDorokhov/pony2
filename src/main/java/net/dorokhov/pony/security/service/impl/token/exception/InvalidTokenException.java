package net.dorokhov.pony.security.service.impl.token.exception;

public final class InvalidTokenException extends Exception {

    public InvalidTokenException() {
        super("User token is invalid.");
    }
}
