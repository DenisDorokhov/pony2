package net.dorokhov.pony2.core.library.service.scan;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.core.library.ProgressObserverFixture;
import net.dorokhov.pony2.core.library.repository.*;
import net.dorokhov.pony2.core.library.service.artwork.ArtworkStorage;
import net.dorokhov.pony2.core.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony2.core.library.service.filetree.domain.ImageNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static net.dorokhov.pony2.core.library.PlatformTransactionManagerFixtures.transactionManager;
import static net.dorokhov.pony2.test.ArtworkFixtures.artwork;
import static net.dorokhov.pony2.test.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BatchLibraryCleanerTest {

    private BatchLibraryCleaner batchLibraryCleaner;

    @TempDir
    private Path tempDir;

    @Mock
    private LibraryCleaner libraryCleaner;
    @Mock
    private SongRepository songRepository;
    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private GenreRepository genreRepository;
    @Mock
    private ArtworkRepository artworkRepository;
    @Mock
    private ArtworkStorage artworkStorage;
    @Mock
    private PlaylistSongRepository playlistSongRepository;
    @Mock
    private PlaybackHistorySongRepository playbackHistorySongRepository;

    private final PlatformTransactionManager transactionManager = transactionManager();

    @BeforeEach
    public void setUp() {
        batchLibraryCleaner = new BatchLibraryCleaner(
                libraryCleaner,
                songRepository,
                albumRepository,
                artistRepository,
                genreRepository,
                artworkRepository,
                artworkStorage,
                playlistSongRepository,
                playbackHistorySongRepository,
                1,
                1,
                transactionManager
        );
    }

    @Test
    public void shouldCleanNotExistingSongs() throws IOException {

        File file1 = Files.createFile(tempDir.resolve("file1.tmp")).toFile();
        File file2 = Files.createFile(tempDir.resolve("file2.tmp")).toFile();
        AudioNode audioNode1 = mock(AudioNode.class);
        AudioNode audioNode2 = mock(AudioNode.class);
        when(audioNode1.getFile()).thenReturn(file1);
        when(audioNode2.getFile()).thenReturn(file2);

        Artwork artwork = artwork();
        Song song1 = song().setId("1").setPath(file1.getAbsolutePath());
        Song song2 = song().setId("2").setPath(file2.getAbsolutePath());
        Song song3 = song().setId("3").setPath("notExistingPath3");
        Song song4 = song().setId("4").setPath("notExistingPath4").setArtwork(artwork);
        Song song5 = song().setId("5").setPath("notExistingPath4");

        when(songRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(
                ImmutableList.of(song1, song2, song3, song4, song5)));
        when(songRepository.findById("3")).thenReturn(Optional.of(song3));
        when(songRepository.findById("4")).thenReturn(Optional.of(song4));
        when(songRepository.findById("5")).thenReturn(Optional.empty());

        ProgressObserverFixture observer = new ProgressObserverFixture();
        batchLibraryCleaner.cleanSongs(ImmutableList.of(audioNode1, audioNode2), observer);

        verify(songRepository, times(2)).delete(any());

        verify(playlistSongRepository).deleteBySongId(eq("3"));
        verify(playbackHistorySongRepository).deleteBySongId(eq("3"));
        verify(songRepository).delete(song3);
        verify(libraryCleaner).deleteArtistIfUnused(song3.getAlbum().getArtist());
        verify(libraryCleaner).deleteAlbumIfUnused(song3.getAlbum());
        verify(libraryCleaner).deleteGenreIfUnused(song3.getGenre());

        verify(playlistSongRepository).deleteBySongId(eq("4"));
        verify(playbackHistorySongRepository).deleteBySongId(eq("4"));
        verify(songRepository).delete(song4);
        verify(libraryCleaner).deleteArtistIfUnused(song4.getAlbum().getArtist());
        verify(libraryCleaner).deleteAlbumIfUnused(song4.getAlbum());
        verify(libraryCleaner).deleteGenreIfUnused(song4.getGenre());
        verify(libraryCleaner).deleteArtworkIfUnused(requireNonNull(song4.getArtwork()));

        assertThat(observer.size()).isEqualTo(3);
        observer.assertThatAt(0, 1, 3);
        observer.assertThatAt(1, 2, 3);
        observer.assertThatAt(2, 3, 3);
    }

    @Test
    public void shouldCleanNotExistingArtworks() throws IOException {

        File file1 = Files.createFile(tempDir.resolve("file1.tmp")).toFile();
        File file2 = Files.createFile(tempDir.resolve("file2.tmp")).toFile();
        ImageNode imageNode1 = mock(ImageNode.class);
        ImageNode imageNode2 = mock(ImageNode.class);
        when(imageNode1.getFile()).thenReturn(file1);
        when(imageNode2.getFile()).thenReturn(file2);

        Artwork artwork1 = artwork()
                .setId("1")
                .setSourceUri(file1.toURI());
        Artwork artwork2 = artwork()
                .setId("2")
                .setDate(LocalDateTime.now().minusDays(1))
                .setSourceUri(file2.toURI());
        Artwork artwork3 = artwork()
                .setId("3")
                .setSourceUri(new File("notExistingPath3").toURI());
        Artwork artwork4 = artwork()
                .setId("4")
                .setSourceUri(new File("notExistingPath4").toURI());
        Artwork artwork5 = artwork()
                .setId("5")
                .setSourceUri(UriComponentsBuilder
                        .fromUriString("http://google.com/logo.png")
                        .build().toUri());

        when(artworkRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(
                ImmutableList.of(artwork1, artwork2, artwork3, artwork4, artwork5)));
        when(artworkRepository.findById("2")).thenReturn(Optional.of(artwork2));
        when(artworkRepository.findById("3")).thenReturn(Optional.of(artwork3));
        when(artworkRepository.findById("4")).thenReturn(Optional.empty());

        ProgressObserverFixture observer = new ProgressObserverFixture();
        batchLibraryCleaner.cleanArtworks(ImmutableList.of(imageNode1, imageNode2), observer);

        verify(artworkStorage, times(2)).delete(any());

        verify(artworkStorage).delete(artwork2.getId());
        verify(songRepository).clearArtworkByArtworkId(artwork2.getId());
        verify(albumRepository).clearArtworkByArtworkId(artwork2.getId());
        verify(artistRepository).clearArtworkByArtworkId(artwork2.getId());
        verify(genreRepository).clearArtworkByArtworkId(artwork2.getId());

        verify(artworkStorage).delete(artwork3.getId());
        verify(songRepository).clearArtworkByArtworkId(artwork3.getId());
        verify(albumRepository).clearArtworkByArtworkId(artwork3.getId());
        verify(artistRepository).clearArtworkByArtworkId(artwork3.getId());
        verify(genreRepository).clearArtworkByArtworkId(artwork3.getId());

        assertThat(observer.size()).isEqualTo(3);
        observer.assertThatAt(0, 1, 3);
        observer.assertThatAt(1, 2, 3);
        observer.assertThatAt(2, 3, 3);
    }

    @Test
    public void shouldCleanNotExistingGenres() {

        Artist artist = new Artist()
                .setSongs(List.of(
                        new Song().setGenre(new Genre().setId("1"))
                ))
                .setGenres(newArrayList(
                        new ArtistGenre()
                                .setId("1_1").setGenre(new Genre().setId("1")),
                        new ArtistGenre()
                                .setId("2_2").setGenre(new Genre().setId("2"))
                ));
        when(artistRepository.findAll()).thenReturn(List.of(artist));

        ProgressObserverFixture observer = new ProgressObserverFixture();
        batchLibraryCleaner.cleanArtistGenres(observer);

        assertThat(artist.getGenres()).hasSize(1);
        assertThat(artist.getGenres().getFirst()).satisfies(genre ->
                assertThat(genre.getId()).isEqualTo("1_1"));

        assertThat(observer.size()).isEqualTo(1);
        observer.assertThatAt(0, 1, 1);
    }

    @Test
    public void shouldNotFailSongsCleanupOnObserverException() {
        when(songRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(ImmutableList.of(song())));
        batchLibraryCleaner.cleanSongs(emptyList(), (itemsComplete, itemsTotal) -> {
            throw new RuntimeException();
        });
    }

    @Test
    public void shouldNotFailArtworksCleanupOnObserverException() {
        when(artworkRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(ImmutableList.of(artwork())));
        batchLibraryCleaner.cleanArtworks(emptyList(), (itemsComplete, itemsTotal) -> {
            throw new RuntimeException();
        });
    }

    @Test
    public void shouldNotFailArtistGenresCleanupOnObserverException() {
        when(artistRepository.findAll()).thenReturn(ImmutableList.of(new Artist()));
        batchLibraryCleaner.cleanArtistGenres((itemsComplete, itemsTotal) -> {
            throw new RuntimeException();
        });
    }
}