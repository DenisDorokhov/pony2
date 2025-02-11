package net.dorokhov.pony2.core.library.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtworkFiles;
import net.dorokhov.pony2.api.library.domain.Genre;
import net.dorokhov.pony2.api.library.domain.Song;
import net.dorokhov.pony2.core.library.repository.ArtistGenreRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.GenreRepository;
import net.dorokhov.pony2.core.library.repository.SongRepository;
import net.dorokhov.pony2.core.library.service.artwork.ArtworkStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static net.dorokhov.pony2.test.ArtworkFixtures.artwork;
import static net.dorokhov.pony2.test.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LibraryServiceImplTest {

    @InjectMocks
    private LibraryServiceImpl libraryService;

    @Mock
    private GenreRepository genreRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private ArtistGenreRepository artistGenreRepository;
    @Mock
    private SongRepository songRepository;
    @Mock
    private ArtworkStorage artworkStorage;
    @Mock
    private RandomFetcher randomFetcher;

    @Test
    public void shouldGetGenres() {

        List<Genre> genres = ImmutableList.of(new Genre(), new Genre());
        when(genreRepository.findAll((Sort) any())).thenReturn(genres);

        assertThat(libraryService.getGenres()).isEqualTo(genres);
    }

    @Test
    public void shouldGetArtists() {

        List<Artist> artists = ImmutableList.of(new Artist(), new Artist());
        when(artistRepository.findAll((Sort) any())).thenReturn(artists);

        assertThat(libraryService.getArtists()).isEqualTo(artists);
    }

    @Test
    public void shouldGetArtistById() {

        Artist artist = new Artist();
        when(artistRepository.findById(any())).thenReturn(Optional.of(artist));

        assertThat(libraryService.getArtistById("1")).containsSame(artist);
    }

    @Test
    public void shouldGetGenreById() {

        Genre genre = new Genre();
        when(genreRepository.findById(any())).thenReturn(Optional.of(genre));

        assertThat(libraryService.getGenreById("1")).containsSame(genre);
    }

    @Test
    public void shouldGetSongById() {

        Song song = song();
        when(songRepository.findById(any())).thenReturn(Optional.ofNullable(song));

        assertThat(libraryService.getSongById("1")).containsSame(song);
    }

    @Test
    public void shouldGetSongsByGenreId() {

        List<Song> songs = ImmutableList.of(song(), song());
        when(songRepository.findPageByGenreId(any(), any())).thenReturn(new PageImpl<>(songs));

        Page<Song> result = libraryService.getSongsByGenreId("1", 0);

        assertThat(result.getContent()).isEqualTo(songs);
    }

    @Test
    public void shouldGetArtworkFilesById() {

        ArtworkFiles artworkFiles = new ArtworkFiles(artwork(), mock(File.class), mock(File.class));
        when(artworkStorage.getArtworkFile(any())).thenReturn(Optional.of(artworkFiles));

        assertThat(libraryService.getArtworkFilesById("1")).containsSame(artworkFiles);
    }

    @Test
    public void shouldGetSongsByIds() {

        List<Song> songs = ImmutableList.of(
                song().setId("1"), song().setId("2"));
        when(songRepository.findAllById(anyIterable())).thenReturn(songs);

        List<Song> result = libraryService.getSongsByIds(ImmutableList.of("1", "2"));

        assertThat(result).isEqualTo(songs);
    }
}