package net.dorokhov.pony2.core.library.service.exception;

public final class PlaylistNotFoundException extends Exception {

    private final String id;

    public PlaylistNotFoundException(String id) {
        super(String.format("Playlist '%s' not found.", id));
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
