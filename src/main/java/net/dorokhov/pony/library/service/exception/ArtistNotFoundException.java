package net.dorokhov.pony.library.service.exception;

public class ArtistNotFoundException extends Exception {

    private final long id;

    public ArtistNotFoundException(Long id) {
        super(String.format("Artist '%d' not found.", id));
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
