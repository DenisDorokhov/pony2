package net.dorokhov.pony.user.service.exception;

public final class TokenSecretNotFoundException extends RuntimeException {

    public TokenSecretNotFoundException() {
        super("Token secret not found.");
    }
}
