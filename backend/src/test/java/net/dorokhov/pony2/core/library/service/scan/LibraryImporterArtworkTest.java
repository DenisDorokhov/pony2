package net.dorokhov.pony2.core.library.service.scan;

import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.core.library.service.filetree.domain.AudioNode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.URI;

import static net.dorokhov.pony2.test.ArtworkFixtures.*;
import static net.dorokhov.pony2.test.ReadableAudioDataFixtures.readableAudioData;
import static net.dorokhov.pony2.test.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LibraryImporterArtworkTest extends AbstractLibraryImporterTest {

    @Test
    public void shouldImportEmbeddedArtwork() throws IOException {

        ArtworkFiles artworkFiles = artworkFiles();
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(artworkFiles);

        Song song = libraryImporter.importAudioData(audioNode(), readableAudioData());

        assertThat(song.getArtwork()).isSameAs(artworkFiles.getArtwork());
    }

    @Test
    public void shouldSkipEmbeddedArtworkWhenImportFailed() throws IOException {

        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenThrow(new IOException());
        Song song = libraryImporter.importAudioData(audioNode(), readableAudioData());


        assertThat(song.getArtwork()).isNull();
    }

    @Test
    public void shouldImportFileArtwork() throws IOException {

        ArtworkFiles artworkFiles = artworkFiles();
        when(libraryArtworkFinder.findAndSaveFileArtwork(any())).thenReturn(artworkFiles);

        Song song = libraryImporter.importAudioData(audioNode(), readableAudioData());

        assertThat(song.getArtwork()).isSameAs(artworkFiles.getArtwork());
    }

    @Test
    public void shouldSkipFileArtworkWhenImportFailed() throws IOException {

        when(libraryArtworkFinder.findAndSaveFileArtwork(any())).thenThrow(new IOException());

        Song song = libraryImporter.importAudioData(audioNode(), readableAudioData());

        assertThat(song.getArtwork()).isNull();
    }

    @Test
    public void shouldClearAlbumArtworkIfArtworkIsDeleted() throws IOException {

        Artwork existingArtwork = artwork().setId("1");
        ReadableAudioData audioData = mockExistingSong(
                song -> song.setArtwork(existingArtwork),
                album -> album.setArtwork(existingArtwork));
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(null);
        when(libraryCleaner.deleteArtworkIfUnused(any())).thenReturn(true);

        libraryImporter.importAudioData(audioNode(), audioData);

        ArgumentCaptor<Album> albumCaptor = ArgumentCaptor.forClass(Album.class);
        verify(albumRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue().getArtwork()).isNull();
    }

    @Test
    public void shouldNotClearAlbumArtworkIfAlbumArtworkIsDifferent() throws IOException {

        Artwork existingArtwork = artwork().setId("1");
        ReadableAudioData audioData = mockExistingSong(
                song -> song.setArtwork(existingArtwork));
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(null);
        when(libraryCleaner.deleteArtworkIfUnused(any())).thenReturn(true);

        libraryImporter.importAudioData(audioNode(), audioData);

        verify(albumRepository, never()).save(any());
    }

    @Test
    public void shouldNotSetAlbumArtworkIfItExists() throws IOException {

        Artwork existingArtwork = artwork().setId("1");
        ReadableAudioData audioData = mockExistingSong(
                song -> song.setArtwork(existingArtwork),
                album -> album.setArtwork(existingArtwork));
        ArtworkFiles newArtworkFiles = artworkFiles(artwork().setId("2"));
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(newArtworkFiles);

        libraryImporter.importAudioData(audioNode(), audioData);

        verify(albumRepository, never()).save(any());
    }

    @Test
    public void shouldUpdateAlbumArtworkIfEmbeddedArtworkChangedAndStillInUse() throws IOException {

        Artwork existingArtwork = artwork().setId("1")
                .setSourceUri(URI.create("embedded:/otherAlbum/song.mp3"));
        ReadableAudioData audioData = mockExistingSong(
                song -> song.setArtwork(existingArtwork),
                album -> album.setArtwork(existingArtwork));
        ArtworkFiles newArtworkFiles = artworkFiles(artwork().setId("2")
                .setSourceUri(URI.create("embedded:/song.mp3")));
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(newArtworkFiles);
        when(libraryCleaner.deleteArtworkIfUnused(existingArtwork)).thenReturn(false);

        Song song = libraryImporter.importAudioData(audioNode(), audioData);

        assertThat(song.getArtwork()).isSameAs(newArtworkFiles.getArtwork());
        verify(libraryCleaner).deleteArtworkIfUnused(existingArtwork);
        ArgumentCaptor<Album> albumCaptor = ArgumentCaptor.forClass(Album.class);
        verify(albumRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue().getArtwork()).isSameAs(newArtworkFiles.getArtwork());
    }

    @Test
    public void shouldNotUpdateAlbumArtworkIfAlbumArtworkIsDifferent() throws IOException {

        Artwork existingArtwork = artwork().setId("1")
                .setSourceUri(URI.create("embedded:/song.mp3"));
        Artwork albumArtwork = artwork().setId("3")
                .setSourceUri(URI.create("embedded:/otherSong.mp3"));
        ReadableAudioData audioData = mockExistingSong(
                song -> song.setArtwork(existingArtwork),
                album -> album.setArtwork(albumArtwork));
        ArtworkFiles newArtworkFiles = artworkFiles(artwork().setId("2")
                .setSourceUri(URI.create("embedded:/song.mp3")));
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(newArtworkFiles);

        libraryImporter.importAudioData(audioNode(), audioData);

        verify(albumRepository, never()).save(any());
    }

    @Test
    public void shouldNotUpdateAlbumArtworkIfItAlreadyMatchesNewArtwork() throws IOException {

        Artwork existingArtwork = artwork().setId("1")
                .setSourceUri(URI.create("embedded:/song.mp3"));
        ArtworkFiles newArtworkFiles = artworkFiles(artwork().setId("2")
                .setSourceUri(URI.create("embedded:/otherSong.mp3")));
        ReadableAudioData audioData = mockExistingSong(
                song -> song.setArtwork(existingArtwork),
                album -> album.setArtwork(newArtworkFiles.getArtwork()));
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(newArtworkFiles);

        libraryImporter.importAudioData(audioNode(), audioData);

        verify(albumRepository, never()).save(any());
    }

    @Test
    public void shouldNotUpdateAlbumArtworkIfSongArtworkIsUnchanged() throws IOException {

        Artwork existingArtwork = artwork().setId("1")
                .setSourceUri(URI.create("embedded:/song.mp3"));
        Artwork albumArtwork = artwork().setId("2")
                .setSourceUri(URI.create("embedded:/otherSong.mp3"));
        ReadableAudioData audioData = mockExistingSong(
                song -> song.setArtwork(existingArtwork),
                album -> album.setArtwork(albumArtwork));
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(artworkFiles(existingArtwork));

        libraryImporter.importAudioData(audioNode(), audioData.setTitle("someTitle"));

        verify(albumRepository, never()).save(any());
        verifyNoInteractions(libraryCleaner);
    }

    @Test
    public void shouldUpdateAlbumArtworkIfSongAlbumAndEmbeddedArtworkChanged() throws IOException {

        Artwork existingArtwork = artwork().setId("1")
                .setSourceUri(URI.create("embedded:/song.mp3"));
        Album existingAlbum = new Album().setId("5")
                .setArtist(new Artist().setId("6"))
                .setArtwork(existingArtwork);
        ReadableAudioData audioData = mockExistingSong(
                song -> song.setAlbum(existingAlbum).setArtwork(existingArtwork),
                album -> album.setArtwork(existingArtwork));
        ArtworkFiles newArtworkFiles = artworkFiles(artwork().setId("2")
                .setSourceUri(URI.create("embedded:/song.mp3")));
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(newArtworkFiles);

        Song song = libraryImporter.importAudioData(audioNode(), audioData);

        ArgumentCaptor<Album> albumCaptor = ArgumentCaptor.forClass(Album.class);
        verify(albumRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue()).isSameAs(song.getAlbum());
        assertThat(albumCaptor.getValue().getArtwork()).isSameAs(newArtworkFiles.getArtwork());
        verify(libraryCleaner).deleteAlbumIfUnused(existingAlbum);
        assertThat(existingAlbum.getArtwork()).isSameAs(existingArtwork);
    }

    @Test
    public void shouldNotReplaceEmbeddedAlbumArtworkWithFileArtwork() throws IOException {

        Artwork existingArtwork = artwork().setId("1")
                .setSourceUri(URI.create("embedded:/song.mp3"));
        ReadableAudioData audioData = mockExistingSong(
                song -> song.setArtwork(existingArtwork),
                album -> album.setArtwork(existingArtwork));
        ArtworkFiles newArtworkFiles = artworkFiles(artwork().setId("2"));
        when(libraryArtworkFinder.findAndSaveFileArtwork(any())).thenReturn(newArtworkFiles);

        libraryImporter.importAudioData(audioNode(), audioData);

        verify(albumRepository, never()).save(any());
    }

    @Test
    public void shouldNotClearAlbumArtworkIfArtworkIsStillInUse() throws IOException {

        Artwork existingArtwork = artwork().setId("1")
                .setSourceUri(URI.create("embedded:/song.mp3"));
        ReadableAudioData audioData = mockExistingSong(
                song -> song.setArtwork(existingArtwork),
                album -> album.setArtwork(existingArtwork));
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(null);
        when(libraryCleaner.deleteArtworkIfUnused(existingArtwork)).thenReturn(false);

        libraryImporter.importAudioData(audioNode(), audioData);

        verify(albumRepository, never()).save(any());
    }

    @Test
    public void shouldFindAndSaveSongAndAlbumArtwork() throws IOException {
        
        when(songRepository.findByPath(any())).thenReturn(song()
                .setArtwork(null)
                .setAlbum(new Album()
                        .setArtist(new Artist())));
        AudioNode audioNode = audioNode();
        ArtworkFiles artworkFiles = artworkFiles();
        when(libraryArtworkFinder.findAndSaveFileArtwork(audioNode)).thenReturn(artworkFiles);
        when(songRepository.save(any())).then(returnsFirstArg());
        
        Song song = libraryImporter.importArtwork(audioNode).orElse(null);
        
        assertThat(song).isNotNull();
        assertThat(song.getArtwork()).isSameAs(artworkFiles.getArtwork());
        verify(songRepository).save(any());
        ArgumentCaptor<Album> albumCaptor = ArgumentCaptor.forClass(Album.class);
        verify(albumRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue().getArtwork()).isSameAs(artworkFiles.getArtwork());
    }

    @Test
    public void shouldNotSaveSongAndAlbumArtworkIfItAlreadyExists() {

        AudioNode audioNode = audioNode();
        Artwork artwork = artwork();
        when(songRepository.findByPath(any())).thenReturn(song()
                .setArtwork(artwork)
                .setAlbum(new Album()
                        .setArtwork(artwork)
                        .setArtist(new Artist())));

        Song song = libraryImporter.importArtwork(audioNode).orElse(null);

        assertThat(song).isNull();
        verify(songRepository, never()).save(any());
        verify(albumRepository, never()).save(any());
    }

    @Test
    public void shouldNotUpdateAlbumArtworkIfUnchangedSongArtworkIsDifferent() {

        Artwork existingArtwork = artwork().setId("1")
                .setSourceUri(URI.create("embedded:/song.mp3"));
        Artwork albumArtwork = artwork().setId("2")
                .setSourceUri(URI.create("embedded:/otherSong.mp3"));
        when(songRepository.findByPath(any())).thenReturn(song()
                .setArtwork(existingArtwork)
                .setAlbum(new Album()
                        .setArtwork(albumArtwork)
                        .setArtist(new Artist())));

        Song song = libraryImporter.importArtwork(audioNode()).orElse(null);

        assertThat(song).isNull();
        verify(songRepository, never()).save(any());
        verify(albumRepository, never()).save(any());
        verifyNoInteractions(libraryArtworkFinder);
    }

    @Test
    public void shouldNotSaveSongAndAlbumArtworkIfItIsNotFound() {

        when(songRepository.findByPath(any())).thenReturn(null);

        Song song = libraryImporter.importArtwork(audioNode()).orElse(null);

        assertThat(song).isNull();
    }

    @Test
    public void shouldNotSaveSongAndAlbumArtworkWhenNoArtworkFound() {
        
        when(songRepository.findByPath(any())).thenReturn(song()
                .setArtwork(null)
                .setAlbum(new Album()
                        .setArtist(new Artist())));

        Song song = libraryImporter.importArtwork(audioNode()).orElse(null);

        assertThat(song).isNull();
        verify(songRepository, never()).save(any());
        verify(albumRepository, never()).save(any());
    }
}
