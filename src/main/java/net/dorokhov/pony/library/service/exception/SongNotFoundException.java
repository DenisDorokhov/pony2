package net.dorokhov.pony.library.service.exception;

public class SongNotFoundException extends Exception {

    private final long id;

    public SongNotFoundException(Long id) {
        super(String.format("Song '%d' not found.", id));
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
