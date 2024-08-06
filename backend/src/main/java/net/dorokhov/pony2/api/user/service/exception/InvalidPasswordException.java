package net.dorokhov.pony2.api.user.service.exception;

public final class InvalidPasswordException extends Exception {
    public InvalidPasswordException() {
        super("Password is invalid.");
    }
}
