package net.dorokhov.pony2.core.library.service.exception;

public final class SongNotFoundException extends Exception {

    private final String id;

    public SongNotFoundException(String id) {
        super(String.format("Song '%s' not found.", id));
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
