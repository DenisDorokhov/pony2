package net.dorokhov.pony.user.exception;

public class TokenSecretNotFoundException extends RuntimeException {

    public TokenSecretNotFoundException() {
        super("Token secret not found.");
    }
}
