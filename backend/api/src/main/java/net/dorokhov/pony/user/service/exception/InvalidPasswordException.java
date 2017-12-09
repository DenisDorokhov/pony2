package net.dorokhov.pony.user.service.exception;

public final class InvalidPasswordException extends Exception {

    public InvalidPasswordException() {
        super("Password is invalid.");
    }
}
