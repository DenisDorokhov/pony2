package net.dorokhov.pony3.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.annotation.Nullable;

import java.io.File;

public final class WritableAudioData extends AbstractAudioData {

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

    public WritableAudioData setWriteDiscNumber(boolean writeDiscNumber) {
        this.writeDiscNumber = writeDiscNumber;
        return this;
    }

    public boolean isWriteDiscCount() {
        return writeDiscCount;
    }

    public WritableAudioData setWriteDiscCount(boolean writeDiscCount) {
        this.writeDiscCount = writeDiscCount;
        return this;
    }

    public boolean isWriteTrackNumber() {
        return writeTrackNumber;
    }

    public WritableAudioData setWriteTrackNumber(boolean writeTrackNumber) {
        this.writeTrackNumber = writeTrackNumber;
        return this;
    }

    public boolean isWriteTrackCount() {
        return writeTrackCount;
    }

    public WritableAudioData setWriteTrackCount(boolean writeTrackCount) {
        this.writeTrackCount = writeTrackCount;
        return this;
    }

    public boolean isWriteTitle() {
        return writeTitle;
    }

    public WritableAudioData setWriteTitle(boolean writeTitle) {
        this.writeTitle = writeTitle;
        return this;
    }

    public boolean isWriteArtist() {
        return writeArtist;
    }

    public WritableAudioData setWriteArtist(boolean writeArtist) {
        this.writeArtist = writeArtist;
        return this;
    }

    public boolean isWriteAlbumArtist() {
        return writeAlbumArtist;
    }

    public WritableAudioData setWriteAlbumArtist(boolean writeAlbumArtist) {
        this.writeAlbumArtist = writeAlbumArtist;
        return this;
    }

    public boolean isWriteAlbum() {
        return writeAlbum;
    }

    public WritableAudioData setWriteAlbum(boolean writeAlbum) {
        this.writeAlbum = writeAlbum;
        return this;
    }

    public boolean isWriteYear() {
        return writeYear;
    }

    public WritableAudioData setWriteYear(boolean writeYear) {
        this.writeYear = writeYear;
        return this;
    }

    public boolean isWriteGenre() {
        return writeGenre;
    }

    public WritableAudioData setWriteGenre(boolean writeGenre) {
        this.writeGenre = writeGenre;
        return this;
    }

    public boolean isWriteArtwork() {
        return writeArtwork;
    }

    public WritableAudioData setWriteArtwork(boolean writeArtwork) {
        this.writeArtwork = writeArtwork;
        return this;
    }

    public @Nullable File getArtworkFile() {
        return artworkFile;
    }

    public WritableAudioData setArtworkFile(@Nullable File artworkFile) {
        this.artworkFile = artworkFile;
        return this;
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
