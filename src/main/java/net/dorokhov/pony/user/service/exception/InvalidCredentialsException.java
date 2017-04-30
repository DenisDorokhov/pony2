package net.dorokhov.pony.user.service.exception;

import static com.google.common.base.Preconditions.checkNotNull;

public class InvalidCredentialsException extends RuntimeException {
    
    private final String email;

    public InvalidCredentialsException(String email) {
        super(String.format("Invalid credentials for user '%s'.", email));
        this.email = checkNotNull(email);
    }

    public String getEmail() {
        return email;
    }
}
