package net.dorokhov.pony.library.service.scan;

import net.dorokhov.pony.api.library.domain.*;
import net.dorokhov.pony.library.service.filetree.domain.AudioNode;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static net.dorokhov.pony.fixture.ArtworkFixtures.*;
import static net.dorokhov.pony.fixture.ReadableAudioDataFixtures.readableAudioDataBuilder;
import static net.dorokhov.pony.fixture.SongFixtures.songBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LibraryImporterArtworkTest extends AbstractLibraryImporterTest {

    @Test
    public void shouldImportEmbeddedArtwork() throws Exception {
        ArtworkFiles artworkFiles = artworkFiles();
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(artworkFiles);
        Song song = libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder().build());
        assertThat(song.getArtwork()).isSameAs(artworkFiles.getArtwork());
    }

    @Test
    public void shouldSkipEmbeddedArtworkWhenImportFailed() throws Exception {
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenThrow(new IOException());
        Song song = libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder().build());
        assertThat(song.getArtwork()).isNull();
    }

    @Test
    public void shouldImportFileArtwork() throws Exception {
        ArtworkFiles artworkFiles = artworkFiles();
        when(libraryArtworkFinder.findAndSaveFileArtwork(any())).thenReturn(artworkFiles);
        Song song = libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder().build());
        assertThat(song.getArtwork()).isSameAs(artworkFiles.getArtwork());
    }

    @Test
    public void shouldSkipFileArtworkWhenImportFailed() throws Exception {
        when(libraryArtworkFinder.findAndSaveFileArtwork(any())).thenThrow(new IOException());
        Song song = libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder().build());
        assertThat(song.getArtwork()).isNull();
    }

    @Test
    public void shouldClearAlbumArtworkIfArtworkIsDeleted() throws Exception {
        Artwork existingArtwork = artworkBuilder().id(1L).build();
        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(
                songBuilder -> songBuilder.artwork(existingArtwork),
                albumBuilder -> albumBuilder.artwork(existingArtwork));
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(null);
        when(libraryCleaner.deleteArtworkIfUnused(any())).thenReturn(true);
        libraryImporter.importAudioData(audioNode(), audioDataBuilder.build());
        ArgumentCaptor<Album> albumCaptor = ArgumentCaptor.forClass(Album.class);
        verify(albumRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue().getArtwork()).isNull();
    }

    @Test
    public void shouldNotClearAlbumArtworkIfAlbumArtworkIsDifferent() throws Exception {
        Artwork existingArtwork = artworkBuilder().id(1L).build();
        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(
                songBuilder -> songBuilder.artwork(existingArtwork));
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(null);
        when(libraryCleaner.deleteArtworkIfUnused(any())).thenReturn(true);
        libraryImporter.importAudioData(audioNode(), audioDataBuilder.build());
        verify(albumRepository, never()).save((Album) any());
    }

    @Test
    public void shouldNotSetAlbumArtworkIfItExists() throws Exception {
        Artwork existingArtwork = artworkBuilder().id(1L).build();
        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(
                songBuilder -> songBuilder.artwork(existingArtwork),
                albumBuilder -> albumBuilder.artwork(existingArtwork));
        ArtworkFiles newArtworkFiles = artworkFiles(artworkBuilder().id(2L).build());
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(newArtworkFiles);
        libraryImporter.importAudioData(audioNode(), audioDataBuilder.build());
        verify(albumRepository, never()).save((Album) any());
    }

    @Test
    public void shouldFindAndSaveSongAndAlbumArtwork() throws Exception {
        
        when(songRepository.findByPath(any())).thenReturn(songBuilder()
                .artwork(null)
                .album(Album.builder()
                        .artist(Artist.builder().build())
                        .build())
                .build());
        AudioNode audioNode = audioNode();
        ArtworkFiles artworkFiles = artworkFiles();
        when(libraryArtworkFinder.findAndSaveFileArtwork(audioNode)).thenReturn(artworkFiles);
        when(songRepository.save((Song) any())).then(returnsFirstArg());
        
        Song song = libraryImporter.importArtwork(audioNode);
        
        assertThat(song).isNotNull();
        assertThat(song.getArtwork()).isSameAs(artworkFiles.getArtwork());
        verify(songRepository).save((Song) any());
        ArgumentCaptor<Album> albumCaptor = ArgumentCaptor.forClass(Album.class);
        verify(albumRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue().getArtwork()).isSameAs(artworkFiles.getArtwork());
    }

    @Test
    public void shouldNotSaveSongAndAlbumArtworkIfItAlreadyExists() throws Exception {

        AudioNode audioNode = audioNode();
        Artwork artwork = artwork();
        when(songRepository.findByPath(any())).thenReturn(songBuilder()
                .artwork(artwork)
                .album(Album.builder()
                        .artwork(artwork)
                        .artist(Artist.builder().build())
                        .build())
                .build());

        Song song = libraryImporter.importArtwork(audioNode);

        assertThat(song).isNotNull();
        verify(songRepository, never()).save((Song) any());
        verify(albumRepository, never()).save((Album) any());
    }

    @Test
    public void shouldNotSaveSongAndAlbumArtworkIfItIsNotFound() throws Exception {
        when(songRepository.findByPath(any())).thenReturn(null);
        Song song = libraryImporter.importArtwork(audioNode());
        assertThat(song).isNull();
    }

    @Test
    public void shouldNotSaveSongAndAlbumArtworkWhenNoArtworkFound() throws Exception {
        
        when(songRepository.findByPath(any())).thenReturn(songBuilder()
                .artwork(null)
                .album(Album.builder()
                        .artist(Artist.builder().build())
                        .build())
                .build());

        Song song = libraryImporter.importArtwork(audioNode());

        assertThat(song).isNotNull();
        verify(songRepository, never()).save((Song) any());
        verify(albumRepository, never()).save((Album) any());
    }
}
