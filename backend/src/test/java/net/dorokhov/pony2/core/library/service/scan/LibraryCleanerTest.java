package net.dorokhov.pony2.core.library.service.scan;

import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.Artwork;
import net.dorokhov.pony2.api.library.domain.Genre;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.GenreRepository;
import net.dorokhov.pony2.core.library.repository.SongRepository;
import net.dorokhov.pony2.core.library.service.artwork.ArtworkStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static net.dorokhov.pony2.test.ArtworkFixtures.artwork;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LibraryCleanerTest {

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
    private ArtworkStorage artworkStorage;

    @BeforeEach
    public void setUp() {
        libraryCleaner = new LibraryCleaner(songRepository, albumRepository, artistRepository, genreRepository, artworkStorage);
    }

    @Test
    public void shouldDeleteArtistIfUnused() {

        Artist artist1 = new Artist().setId("1");
        Artist artist2 = new Artist().setId("2");

        when(albumRepository.countByArtistId("1")).thenReturn(0L);
        when(albumRepository.countByArtistId("2")).thenReturn(1L);

        assertThat(libraryCleaner.deleteArtistIfUnused(artist1)).isTrue();
        verify(artistRepository).deleteById("1");

        assertThat(libraryCleaner.deleteArtistIfUnused(artist2)).isFalse();
        verify(artistRepository, never()).deleteById("2");
    }

    @Test
    public void shouldDeleteAlbumIfUnused() {

        Artist artist = new Artist();

        Album album1 = new Album().setId("1").setArtist(artist);
        Album album2 = new Album().setId("2").setArtist(artist);

        when(songRepository.countByAlbumId("1")).thenReturn(0L);
        when(songRepository.countByAlbumId("2")).thenReturn(1L);

        assertThat(libraryCleaner.deleteAlbumIfUnused(album1)).isTrue();
        verify(albumRepository).deleteById("1");

        assertThat(libraryCleaner.deleteAlbumIfUnused(album2)).isFalse();
        verify(albumRepository, never()).deleteById("2");
    }

    @Test
    public void shouldDeleteGenreIfUnused() {

        Genre genre1 = new Genre().setId("1");
        Genre genre2 = new Genre().setId("2");

        when(songRepository.countByGenreId("1")).thenReturn(0L);
        when(songRepository.countByGenreId("2")).thenReturn(1L);

        assertThat(libraryCleaner.deleteGenreIfUnused(genre1)).isTrue();
        verify(genreRepository).deleteById("1");

        assertThat(libraryCleaner.deleteGenreIfUnused(genre2)).isFalse();
        verify(genreRepository, never()).deleteById("2");
    }

    @Test
    public void shouldDeleteArtworkIfUnused() {

        Artwork artwork1 = artwork().setId("1");
        Artwork artwork2 = artwork().setId("2");

        when(songRepository.countByArtworkId("1")).thenReturn(0L);
        when(songRepository.countByArtworkId("2")).thenReturn(1L);

        assertThat(libraryCleaner.deleteArtworkIfUnused(artwork1)).isTrue();
        verify(songRepository).clearArtworkByArtworkId("1");
        verify(albumRepository).clearArtworkByArtworkId("1");
        verify(artistRepository).clearArtworkByArtworkId("1");
        verify(genreRepository).clearArtworkByArtworkId("1");
        verify(artworkStorage).delete("1");

        assertThat(libraryCleaner.deleteArtworkIfUnused(artwork2)).isFalse();
        verify(songRepository, never()).clearArtworkByArtworkId("2");
        verify(albumRepository, never()).clearArtworkByArtworkId("2");
        verify(artistRepository, never()).clearArtworkByArtworkId("2");
        verify(genreRepository, never()).clearArtworkByArtworkId("2");
        verify(artworkStorage, never()).delete("2");
    }
}