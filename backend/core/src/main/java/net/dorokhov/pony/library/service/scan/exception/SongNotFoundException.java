package net.dorokhov.pony.library.service.scan.exception;

import static com.google.common.base.Preconditions.checkNotNull;

public class SongNotFoundException extends Exception {
    
    private final Long songId;

    public SongNotFoundException(Long songId) {
        super(String.format("Song '%d' not found.", songId));
        this.songId = checkNotNull(songId);
    }

    public Long getSongId() {
        return songId;
    }
}
