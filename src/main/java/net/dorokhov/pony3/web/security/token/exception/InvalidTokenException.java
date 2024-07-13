package net.dorokhov.pony3.web.security.token.exception;

public final class InvalidTokenException extends Exception {
    public InvalidTokenException() {
        super("User token is invalid.");
    }
}
