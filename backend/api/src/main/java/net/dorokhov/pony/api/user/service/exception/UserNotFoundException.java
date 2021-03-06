package net.dorokhov.pony.api.user.service.exception;

public final class UserNotFoundException extends Exception {
    
    private final String id;

    public UserNotFoundException(String id) {
        super(String.format("User '%s' not found.", id));
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
