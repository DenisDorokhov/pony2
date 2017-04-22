package net.dorokhov.pony.user.exception;

import static com.google.common.base.Preconditions.checkNotNull;

public class DeletingCurrentUserException extends RuntimeException {
    
    private final Long id;

    public DeletingCurrentUserException(Long id) {
        super(String.format("Deleting current user '%d' is not allowed.", id));
        checkNotNull(id);
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
