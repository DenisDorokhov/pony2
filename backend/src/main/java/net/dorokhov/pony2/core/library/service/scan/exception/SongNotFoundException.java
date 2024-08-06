package net.dorokhov.pony2.core.library.service.scan.exception;

import static com.google.common.base.Preconditions.checkNotNull;

public class SongNotFoundException extends Exception {
    
    private final String songId;

    public SongNotFoundException(String songId) {
        super(String.format("Song '%s' not found.", songId));
        this.songId = checkNotNull(songId);
    }

    public String getSongId() {
        return songId;
    }
}
