package net.dorokhov.pony3.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.annotation.Nullable;

import java.io.File;

public final class WritableAudioData extends AbstractAudioData<WritableAudioData> {

    private boolean writeDiscNumber;
    private boolean writeDiscCount;

    private boolean writeTrackNumber;
    private boolean writeTrackCount;

    private boolean writeTitle;
    private boolean writeArtist;
    private boolean writeAlbumArtist;
    private boolean writeAlbum;

    private boolean writeYear;

    private boolean writeGenre;

    private boolean writeArtwork;
    private File artworkFile;

    public boolean isWriteDiscNumber() {
        return writeDiscNumber;
    }

    public boolean isWriteDiscCount() {
        return writeDiscCount;
    }

    public boolean isWriteTrackNumber() {
        return writeTrackNumber;
    }

    public boolean isWriteTrackCount() {
        return writeTrackCount;
    }

    public boolean isWriteTitle() {
        return writeTitle;
    }

    public boolean isWriteArtist() {
        return writeArtist;
    }

    public boolean isWriteAlbumArtist() {
        return writeAlbumArtist;
    }

    public boolean isWriteAlbum() {
        return writeAlbum;
    }

    public boolean isWriteYear() {
        return writeYear;
    }

    public boolean isWriteGenre() {
        return writeGenre;
    }

    public boolean isWriteArtwork() {
        return writeArtwork;
    }

    public @Nullable File getArtworkFile() {
        return artworkFile;
    }

    public WritableAudioData setArtworkFile(@Nullable File artworkFile) {
        this.artworkFile = artworkFile;
        writeArtwork = true;
        return this;
    }

    @Override
    public WritableAudioData setDiscNumber(@Nullable Integer discNumber) {
        writeDiscNumber = true;
        return super.setDiscNumber(discNumber);
    }

    @Override
    public WritableAudioData setDiscCount(Integer discCount) {
        writeDiscCount = true;
        return super.setDiscCount(discCount);
    }

    @Override
    public WritableAudioData setTrackNumber(Integer trackNumber) {
        writeTrackNumber = true;
        return super.setTrackNumber(trackNumber);
    }

    @Override
    public WritableAudioData setTrackCount(Integer trackCount) {
        writeTrackCount = true;
        return super.setTrackCount(trackCount);
    }

    @Override
    public WritableAudioData setTitle(String title) {
        writeTitle = true;
        return super.setTitle(title);
    }

    @Override
    public WritableAudioData setArtist(String artist) {
        writeArtist = true;
        return super.setArtist(artist);
    }

    @Override
    public WritableAudioData setAlbumArtist(String albumArtist) {
        writeAlbumArtist = true;
        return super.setAlbumArtist(albumArtist);
    }

    @Override
    public WritableAudioData setAlbum(String album) {
        writeAlbum = true;
        return super.setAlbum(album);
    }

    @Override
    public WritableAudioData setYear(Integer year) {
        writeYear = true;
        return super.setYear(year);
    }

    @Override
    public WritableAudioData setGenre(String genre) {
        writeGenre = true;
        return super.setGenre(genre);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("writeDiscNumber", writeDiscNumber)
                .add("writeDiscCount", writeDiscCount)
                .add("writeTrackNumber", writeTrackNumber)
                .add("writeTrackCount", writeTrackCount)
                .add("writeTitle", writeTitle)
                .add("writeArtist", writeArtist)
                .add("writeAlbumArtist", writeAlbumArtist)
                .add("writeAlbum", writeAlbum)
                .add("writeYear", writeYear)
                .add("writeGenre", writeGenre)
                .add("writeArtwork", writeArtwork)
                .add("artworkFile", artworkFile)
                .add("discNumber", discNumber)
                .add("discCount", discCount)
                .add("trackNumber", trackNumber)
                .add("trackCount", trackCount)
                .add("title", title)
                .add("artist", artist)
                .add("albumArtist", albumArtist)
                .add("album", album)
                .add("year", year)
                .add("genre", genre)
                .toString();
    }
}
