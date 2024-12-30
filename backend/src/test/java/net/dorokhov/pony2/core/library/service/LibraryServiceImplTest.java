package net.dorokhov.pony2.core.library.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.GenreRepository;
import net.dorokhov.pony2.core.library.repository.SongRepository;
import net.dorokhov.pony2.core.library.service.artwork.ArtworkStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony2.test.ArtworkFixtures.artwork;
import static net.dorokhov.pony2.test.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LibraryServiceImplTest {

    @InjectMocks
    private LibraryServiceImpl libraryService;

    @Mock
    private GenreRepository genreRepository;
    @Mock
    private ArtistRepository artistRepository;
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

    @Test
    @SuppressWarnings("unchecked")
    public void shouldGetRandomSongs() {

        when(songRepository.findByGenreIdIn(any(), any())).thenReturn(emptyList());
        List<Song> songs = new ArrayList<>();
        when(randomFetcher.fetch(eq(5), (RandomFetcher.Repository<Song>) any())).thenAnswer(invocation -> {
            RandomFetcher.Repository<?> repository = invocation.getArgument(1);
            repository.fetchCount();
            repository.fetchContent(PageRequest.of(0, 1));
            return songs;
        });

        assertThat(libraryService.getRandomSongs(new RandomSongsRequest()
                .setLastArtistId(null)
                .setGenreIds(List.of("1"))
                .setCount(5))
        ).isSameAs(songs);

        verify(songRepository).countByGenreIdIn(eq(List.of("1")));
        verify(songRepository).findByGenreIdIn(eq(List.of("1")), any());
    }
}