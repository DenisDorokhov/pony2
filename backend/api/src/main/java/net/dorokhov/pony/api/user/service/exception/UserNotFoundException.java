package net.dorokhov.pony.api.user.service.exception;

public final class UserNotFoundException extends Exception {
    
    private final long id;

    public UserNotFoundException(Long id) {
        super(String.format("User '%d' not found.", id));
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
