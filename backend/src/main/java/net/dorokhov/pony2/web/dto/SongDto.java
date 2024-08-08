package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Song;

public final class SongDto extends BaseDto<SongDto> {

    private String path;
    private String mimeType;
    private String fileExtension;

    private Long size;
    private Long duration;

    private Long bitRate;
    private Boolean bitRateVariable;

    private Integer discNumber;
    private Integer trackNumber;

    private String name;

    private String artistName;

    private String albumId;
    private String genreId;

    public String getPath() {
        return path;
    }

    public SongDto setPath(String path) {
        this.path = path;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public SongDto setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public SongDto setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public SongDto setSize(Long size) {
        this.size = size;
        return this;
    }

    public Long getDuration() {
        return duration;
    }

    public SongDto setDuration(Long duration) {
        this.duration = duration;
        return this;
    }

    public Long getBitRate() {
        return bitRate;
    }

    public SongDto setBitRate(Long bitRate) {
        this.bitRate = bitRate;
        return this;
    }

    public Boolean getBitRateVariable() {
        return bitRateVariable;
    }

    public SongDto setBitRateVariable(Boolean bitRateVariable) {
        this.bitRateVariable = bitRateVariable;
        return this;
    }

    @Nullable
    public Integer getDiscNumber() {
        return discNumber;
    }

    public SongDto setDiscNumber(@Nullable Integer discNumber) {
        this.discNumber = discNumber;
        return this;
    }

    @Nullable
    public Integer getTrackNumber() {
        return trackNumber;
    }

    public SongDto setTrackNumber(@Nullable Integer trackNumber) {
        this.trackNumber = trackNumber;
        return this;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public SongDto setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getArtistName() {
        return artistName;
    }

    public SongDto setArtistName(@Nullable String artistName) {
        this.artistName = artistName;
        return this;
    }

    public String getAlbumId() {
        return albumId;
    }

    public SongDto setAlbumId(String albumId) {
        this.albumId = albumId;
        return this;
    }

    public String getGenreId() {
        return genreId;
    }

    public SongDto setGenreId(String genreId) {
        this.genreId = genreId;
        return this;
    }

    public static SongDto of(Song song, boolean isAdmin) {
        return new SongDto()
                .setId(song.getId())
                .setCreationDate(song.getCreationDate())
                .setUpdateDate(song.getUpdateDate())
                .setPath(isAdmin ? song.getPath() : null)
                .setMimeType(song.getFileType().getMimeType())
                .setFileExtension(song.getFileType().getFileExtension())
                .setSize(song.getSize())
                .setDuration(song.getDuration())
                .setBitRate(song.getBitRate())
                .setBitRateVariable(song.getBitRateVariable())
                .setDiscNumber(song.getDiscNumber())
                .setTrackNumber(song.getTrackNumber())
                .setName(song.getName())
                .setArtistName(song.getArtistName())
                .setAlbumId(song.getAlbum().getId())
                .setGenreId(song.getGenre().getId());
    }
}
