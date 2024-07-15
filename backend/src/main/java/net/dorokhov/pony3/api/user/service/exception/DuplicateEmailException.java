package net.dorokhov.pony3.api.user.service.exception;

import static com.google.common.base.Preconditions.checkNotNull;

public final class DuplicateEmailException extends Exception {

    private final String email;
    
    public DuplicateEmailException(String email) {
        super(String.format("User with email '%s' already exists.", email));
        this.email = checkNotNull(email);
    }

    public String getEmail() {
        return email;
    }
}
