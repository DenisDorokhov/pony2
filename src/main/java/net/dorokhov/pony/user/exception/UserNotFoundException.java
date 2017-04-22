package net.dorokhov.pony.user.exception;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserNotFoundException extends RuntimeException {
    
    private final Long id;

    public UserNotFoundException(Long id) {
        super(String.format("User '%d' not found.", id));
        checkNotNull(id);
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
