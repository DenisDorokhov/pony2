package net.dorokhov.pony.user.service.exception;

public final class TokenSecretNotFoundException extends Exception {

    public TokenSecretNotFoundException() {
        super("Token secret not found.");
    }
}
