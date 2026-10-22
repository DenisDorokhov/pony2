package net.dorokhov.pony2.core.library.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtistDiscoveryData;
import net.dorokhov.pony2.api.library.domain.ArtistDiscoveryIndex;
import net.dorokhov.pony2.api.library.domain.ArtworkFiles;
import net.dorokhov.pony2.api.library.domain.Genre;
import net.dorokhov.pony2.api.library.domain.MusicBrainzAlbumData;
import net.dorokhov.pony2.api.library.domain.Song;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.repository.ArtistDiscoveryIndexRepository;
import net.dorokhov.pony2.core.library.repository.ArtistGenreRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.GenreRepository;
import net.dorokhov.pony2.core.library.repository.SongRepository;
import net.dorokhov.pony2.core.library.service.artwork.ArtworkStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static net.dorokhov.pony2.test.ArtworkFixtures.artwork;
import static net.dorokhov.pony2.test.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LibraryServiceImplTest {

    @InjectMocks
    private LibraryServiceImpl libraryService;

    @Mock
    private net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository discoveryTaskRepository;
    @Mock
    private GenreRepository genreRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private ArtistGenreRepository artistGenreRepository;
    @Mock
    private ArtistDiscoveryIndexRepository artistDiscoveryIndexRepository;
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

    @ParameterizedTest
    @CsvSource(value = {
            "Album||LP",
            "EP||EP",
            "Single||SINGLE",
            "Broadcast||UNKNOWN",
            "Other|Soundtrack|UNKNOWN",
            "||UNKNOWN",
            "Album|Soundtrack|LP",
            "EP|Remix|EP",
            "Single|Live|LIVE",
            "Album|Compilation|COMPILATION",
            "Album|Live;Compilation|COMPILATION",
            "Album|Compilation;Live|COMPILATION",
            "|Live|LIVE"
    }, delimiter = '|')
    void shouldDetermineAlbumReleaseType(String primaryType, String secondaryTypes, AlbumDiscoveryData.ReleaseType expected) {
        MusicBrainzAlbumData data = new MusicBrainzAlbumData("release-group", primaryType,
                secondaryTypes != null ? List.of(secondaryTypes.split(";")) : List.of(), "1986-10-07", "artist", List.of());
        when(discoveryTaskRepository.findLatestCompletedMusicBrainzResultByAlbumId(eq("album"), any(Limit.class)))
                .thenReturn(Optional.of(JsonConverter.toJson(data)));

        assertThat(libraryService.getAlbumDiscoveryData("album"))
                .isEqualTo(new AlbumDiscoveryData(expected, LocalDateTime.of(1986, 10, 7, 0, 0)));
    }

    @ParameterizedTest
    @CsvSource({
            "1986-10-07, 1986-10-07T00:00",
            "1986-10, 1986-10-01T00:00",
            "1986, 1986-01-01T00:00",
            "2000-02-29, 2000-02-29T00:00"
    })
    void shouldDetermineAlbumReleaseDate(String firstReleaseDate, String expected) {
        MusicBrainzAlbumData data = new MusicBrainzAlbumData("release-group", "Album", List.of(), firstReleaseDate, "artist", List.of());
        when(discoveryTaskRepository.findLatestCompletedMusicBrainzResultByAlbumId(eq("album"), any(Limit.class)))
                .thenReturn(Optional.of(JsonConverter.toJson(data)));

        assertThat(libraryService.getAlbumDiscoveryData("album").releaseDate()).isEqualTo(LocalDateTime.parse(expected));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "not a date", "1986-02-29", "1986-13", "1986-10-32", "1986-00", "1986-1", "1986-10-07T12:00"})
    void shouldReturnNullWhenAlbumReleaseDateIsMissingOrInvalid(String firstReleaseDate) {
        MusicBrainzAlbumData data = new MusicBrainzAlbumData("release-group", "Album", List.of(), firstReleaseDate, "artist", List.of());
        when(discoveryTaskRepository.findLatestCompletedMusicBrainzResultByAlbumId(eq("album"), any(Limit.class)))
                .thenReturn(Optional.of(JsonConverter.toJson(data)));

        assertThat(libraryService.getAlbumDiscoveryData("album"))
                .isEqualTo(new AlbumDiscoveryData(AlbumDiscoveryData.ReleaseType.LP, null));
    }

    @Test
    void shouldReturnNullWhenAlbumReleaseDateFieldIsMissing() {
        String result = """
                {"releaseGroupId":"release-group","primaryType":"Album","secondaryTypes":[],"artistId":"artist","country":[]}
                """;
        when(discoveryTaskRepository.findLatestCompletedMusicBrainzResultByAlbumId(eq("album"), any(Limit.class)))
                .thenReturn(Optional.of(result));

        assertThat(libraryService.getAlbumDiscoveryData("album"))
                .isEqualTo(new AlbumDiscoveryData(AlbumDiscoveryData.ReleaseType.LP, null));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "null")
    void shouldReturnUnknownWhenAlbumDiscoveryDataIsMissing(String result) {
        when(discoveryTaskRepository.findLatestCompletedMusicBrainzResultByAlbumId(eq("album"), any(Limit.class)))
                .thenReturn(Optional.ofNullable(result));

        assertThat(libraryService.getAlbumDiscoveryData("album"))
                .isEqualTo(new AlbumDiscoveryData(AlbumDiscoveryData.ReleaseType.UNKNOWN, null));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldReturnExistingArtistTopSongsInOriginalOrder(boolean countryCodeAvailable) {
        when(discoveryTaskRepository.findLatestCompletedSpotifyTopTracksResultByArtistId(eq("artist"), any(Limit.class)))
                .thenReturn(Optional.of("[\"second\",null,\"deleted\",\"first\"]"));
        when(songRepository.findExistingIds(List.of("second", "deleted", "first"))).thenReturn(List.of("first", "second"));
        if (countryCodeAvailable) {
            when(artistDiscoveryIndexRepository.findByArtistId("artist"))
                    .thenReturn(Optional.of(new ArtistDiscoveryIndex().setCountryCode("DE")));
        }

        assertThat(libraryService.getArtistDiscoveryData("artist"))
                .isEqualTo(new ArtistDiscoveryData(countryCodeAvailable ? "DE" : null, List.of("second", "first")));
    }

    @Test
    void shouldReturnDistinctArtistTopSongsInOriginalOrder() {
        when(discoveryTaskRepository.findLatestCompletedSpotifyTopTracksResultByArtistId(eq("artist"), any(Limit.class)))
                .thenReturn(Optional.of("[\"second\",null,\"deleted\",\"first\",\"second\",\"first\"]"));
        when(songRepository.findExistingIds(any())).thenReturn(List.of("first", "second"));

        assertThat(libraryService.getArtistDiscoveryData("artist").topSongIds()).containsExactly("second", "first");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"null", "[]", "[null]"})
    void shouldReturnEmptyTopSongsWhenDiscoveryResultIsMissingOrEmpty(String result) {
        when(discoveryTaskRepository.findLatestCompletedSpotifyTopTracksResultByArtistId(eq("artist"), any(Limit.class)))
                .thenReturn(Optional.ofNullable(result));

        assertThat(libraryService.getArtistDiscoveryData("artist").topSongIds()).isEmpty();
        verifyNoInteractions(songRepository);
    }

    @Test
    void shouldReturnEmptyTopSongsWhenAllMatchesAreDeleted() {
        when(discoveryTaskRepository.findLatestCompletedSpotifyTopTracksResultByArtistId(eq("artist"), any(Limit.class)))
                .thenReturn(Optional.of("[\"deleted\"]"));
        when(songRepository.findExistingIds(List.of("deleted"))).thenReturn(List.of());

        assertThat(libraryService.getArtistDiscoveryData("artist").topSongIds()).isEmpty();
    }

    @Test
    public void shouldGetSongById() {

        Song song = song();
        when(songRepository.findById(any())).thenReturn(Optional.ofNullable(song));

        assertThat(libraryService.getSongById("1")).containsSame(song);
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
