package net.dorokhov.pony.user.exception;

public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException() {
        super("Entered password is invalid.");
    }
}
