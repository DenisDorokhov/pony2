package net.dorokhov.pony.user.service.exception;

public final class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException() {
        super("Entered password is invalid.");
    }
}
