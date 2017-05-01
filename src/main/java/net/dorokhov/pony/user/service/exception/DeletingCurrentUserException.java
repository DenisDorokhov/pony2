package net.dorokhov.pony.user.service.exception;

public final class DeletingCurrentUserException extends RuntimeException {
    
    private final long id;

    public DeletingCurrentUserException(long id) {
        super(String.format("Deleting current user '%d' is not allowed.", id));
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
