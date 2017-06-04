package net.dorokhov.pony.library.service.impl.scan;

import net.dorokhov.pony.library.domain.Album;
import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.Artwork;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.service.impl.audio.domain.ReadableAudioData;
import net.dorokhov.pony.library.service.impl.filetree.domain.AudioNode;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static net.dorokhov.pony.fixture.ArtworkFixtures.artwork;
import static net.dorokhov.pony.fixture.ArtworkFixtures.artworkBuilder;
import static net.dorokhov.pony.fixture.SongFixtures.songBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class LibraryImporterArtworkTest extends AbstractLibraryImporterTest {

    @Test
    public void shouldImportEmbeddedArtwork() throws Exception {
        Artwork artwork = artwork();
        given(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).willReturn(artwork);
        Song song = libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder().build());
        assertThat(song.getArtwork()).isSameAs(artwork);
    }

    @Test
    public void shouldSkipEmbeddedArtworkWhenImportFailed() throws Exception {
        given(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).willThrow(new IOException());
        Song song = libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder().build());
        assertThat(song.getArtwork()).isNull();
    }

    @Test
    public void shouldImportFileArtwork() throws Exception {
        Artwork artwork = artwork();
        given(libraryArtworkFinder.findAndSaveFileArtwork(any())).willReturn(artwork);
        Song song = libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder().build());
        assertThat(song.getArtwork()).isSameAs(artwork);
    }

    @Test
    public void shouldSkipFileArtworkWhenImportFailed() throws Exception {
        given(libraryArtworkFinder.findAndSaveFileArtwork(any())).willThrow(new IOException());
        Song song = libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder().build());
        assertThat(song.getArtwork()).isNull();
    }

    @Test
    public void shouldClearAlbumArtworkIfArtworkIsDeleted() throws Exception {
        Artwork existingArtwork = artworkBuilder().id(1L).build();
        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(
                songBuilder -> songBuilder.artwork(existingArtwork),
                albumBuilder -> albumBuilder.artwork(existingArtwork));
        given(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).willReturn(null);
        given(libraryCleaner.deleteArtworkIfUnused(any())).willReturn(true);
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
        given(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).willReturn(null);
        given(libraryCleaner.deleteArtworkIfUnused(any())).willReturn(true);
        libraryImporter.importAudioData(audioNode(), audioDataBuilder.build());
        verify(albumRepository, never()).save((Album) any());
    }

    @Test
    public void shouldNotSetAlbumArtworkIfItExists() throws Exception {
        Artwork existingArtwork = artworkBuilder().id(1L).build();
        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(
                songBuilder -> songBuilder.artwork(existingArtwork),
                albumBuilder -> albumBuilder.artwork(existingArtwork));
        Artwork newArtwork = artworkBuilder().id(2L).build();
        given(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).willReturn(newArtwork);
        libraryImporter.importAudioData(audioNode(), audioDataBuilder.build());
        verify(albumRepository, never()).save((Album) any());
    }

    @Test
    public void shouldFindAndSaveSongAndAlbumArtwork() throws Exception {
        
        AudioNode audioNode = audioNode();
        Artwork artwork = artwork();
        given(songRepository.findByPath(any())).willReturn(songBuilder()
                .artwork(null)
                .album(Album.builder()
                        .artist(Artist.builder().build())
                        .build())
                .build());
        given(libraryArtworkFinder.findAndSaveFileArtwork(audioNode)).willReturn(artwork);
        given(songRepository.save((Song) any())).willAnswer(returnsFirstArg());
        
        Song song = libraryImporter.importArtwork(audioNode);
        
        assertThat(song).isNotNull();
        assertThat(song.getArtwork()).isSameAs(artwork);
        verify(songRepository).save((Song) any());
        ArgumentCaptor<Album> albumCaptor = ArgumentCaptor.forClass(Album.class);
        verify(albumRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue().getArtwork()).isSameAs(artwork);
    }

    @Test
    public void shouldNotSaveSongAndAlbumArtworkIfItAlreadyExists() throws Exception {

        AudioNode audioNode = audioNode();
        Artwork artwork = artwork();
        given(songRepository.findByPath(any())).willReturn(songBuilder()
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
        given(songRepository.findByPath(any())).willReturn(null);
        Song song = libraryImporter.importArtwork(audioNode());
        assertThat(song).isNull();
    }

    @Test
    public void shouldNotSaveSongAndAlbumArtworkWhenNoArtworkFound() throws Exception {
        
        given(songRepository.findByPath(any())).willReturn(songBuilder()
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
