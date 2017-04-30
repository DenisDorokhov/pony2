package net.dorokhov.pony.user.service.exception;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserExistsException extends RuntimeException {

    private final String email;
    
    public UserExistsException(String email) {
        super(String.format("User with email '%s' already exists.", email));
        this.email = checkNotNull(email);
    }

    public String getEmail() {
        return email;
    }
}
