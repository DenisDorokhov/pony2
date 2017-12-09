package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.library.domain.Song;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public final class SongDto extends BaseDto {

    private final String mimeType;
    private final Long size;
    private final Long duration;

    private final Long bitRate;
    private final Boolean bitRateVariable;

    private final Integer discNumber;
    private final Integer trackNumber;

    private final String name;

    private final String artistName;

    private final Long album;

    private final Long genre;

    SongDto(Long id, LocalDateTime creationDate, @Nullable LocalDateTime updateDate,
            String mimeType, Long size, Long duration,
            Long bitRate, Boolean bitRateVariable,
            @Nullable Integer discNumber, @Nullable Integer trackNumber,
            @Nullable String name, @Nullable String artistName,
            Long album, Long genre) {
        super(id, creationDate, updateDate);
        this.mimeType = checkNotNull(mimeType);
        this.size = checkNotNull(size);
        this.duration = checkNotNull(duration);
        this.bitRate = checkNotNull(bitRate);
        this.bitRateVariable = checkNotNull(bitRateVariable);
        this.discNumber = discNumber;
        this.trackNumber = trackNumber;
        this.name = name;
        this.artistName = artistName;
        this.album = checkNotNull(album);
        this.genre = checkNotNull(genre);
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getSize() {
        return size;
    }

    public Long getDuration() {
        return duration;
    }

    public Long getBitRate() {
        return bitRate;
    }

    public Boolean isBitRateVariable() {
        return bitRateVariable;
    }

    @Nullable
    public Integer getDiscNumber() {
        return discNumber;
    }

    @Nullable
    public Integer getTrackNumber() {
        return trackNumber;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getArtistName() {
        return artistName;
    }

    public Long getAlbum() {
        return album;
    }

    public Long getGenre() {
        return genre;
    }

    public static SongDto of(Song song) {
        return new SongDto(song.getId(), song.getCreationDate(), song.getUpdateDate(),
                song.getFileType().getMimeType(), song.getSize(), song.getDuration(),
                song.getBitRate(), song.isBitRateVariable(),
                song.getDiscNumber(), song.getTrackNumber(),
                song.getName(),
                song.getArtistName(),
                song.getAlbum().getId(), song.getGenre().getId());
    }
}
