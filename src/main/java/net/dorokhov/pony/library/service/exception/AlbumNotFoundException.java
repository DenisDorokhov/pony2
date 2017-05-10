package net.dorokhov.pony.library.service.exception;

public class AlbumNotFoundException extends Exception {

    private final long id;

    public AlbumNotFoundException(Long id) {
        super(String.format("Album '%d' not found.", id));
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
