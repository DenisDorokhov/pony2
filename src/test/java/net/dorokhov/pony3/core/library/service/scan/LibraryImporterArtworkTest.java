package net.dorokhov.pony3.core.library.service.scan;

import net.dorokhov.pony3.api.library.domain.*;
import net.dorokhov.pony3.core.library.service.filetree.domain.AudioNode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static net.dorokhov.pony3.test.ArtworkFixtures.*;
import static net.dorokhov.pony3.test.ReadableAudioDataFixtures.readableAudioData;
import static net.dorokhov.pony3.test.SongFixtures.song;
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
        ReadableAudioData audioDataBuilder = mockExistingSong(
                songBuilder -> songBuilder.setArtwork(existingArtwork),
                albumBuilder -> albumBuilder.setArtwork(existingArtwork));
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(null);
        when(libraryCleaner.deleteArtworkIfUnused(any())).thenReturn(true);

        libraryImporter.importAudioData(audioNode(), audioDataBuilder);

        ArgumentCaptor<Album> albumCaptor = ArgumentCaptor.forClass(Album.class);
        verify(albumRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue().getArtwork()).isNull();
    }

    @Test
    public void shouldNotClearAlbumArtworkIfAlbumArtworkIsDifferent() throws IOException {

        Artwork existingArtwork = artwork().setId("1");
        ReadableAudioData audioDataBuilder = mockExistingSong(
                songBuilder -> songBuilder.setArtwork(existingArtwork));
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(null);
        when(libraryCleaner.deleteArtworkIfUnused(any())).thenReturn(true);

        libraryImporter.importAudioData(audioNode(), audioDataBuilder);

        verify(albumRepository, never()).save(any());
    }

    @Test
    public void shouldNotSetAlbumArtworkIfItExists() throws IOException {

        Artwork existingArtwork = artwork().setId("1");
        ReadableAudioData audioDataBuilder = mockExistingSong(
                songBuilder -> songBuilder.setArtwork(existingArtwork),
                albumBuilder -> albumBuilder.setArtwork(existingArtwork));
        ArtworkFiles newArtworkFiles = artworkFiles(artwork().setId("2"));
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(newArtworkFiles);

        libraryImporter.importAudioData(audioNode(), audioDataBuilder);

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
        
        Song song = libraryImporter.importArtwork(audioNode);
        
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

        Song song = libraryImporter.importArtwork(audioNode);

        assertThat(song).isNotNull();
        verify(songRepository, never()).save(any());
        verify(albumRepository, never()).save(any());
    }

    @Test
    public void shouldNotSaveSongAndAlbumArtworkIfItIsNotFound() {

        when(songRepository.findByPath(any())).thenReturn(null);

        Song song = libraryImporter.importArtwork(audioNode());

        assertThat(song).isNull();
    }

    @Test
    public void shouldNotSaveSongAndAlbumArtworkWhenNoArtworkFound() {
        
        when(songRepository.findByPath(any())).thenReturn(song()
                .setArtwork(null)
                .setAlbum(new Album()
                        .setArtist(new Artist())));

        Song song = libraryImporter.importArtwork(audioNode());

        assertThat(song).isNotNull();
        verify(songRepository, never()).save(any());
        verify(albumRepository, never()).save(any());
    }
}
