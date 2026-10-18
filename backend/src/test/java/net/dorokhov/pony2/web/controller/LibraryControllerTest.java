package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.ApiTemplate;
import net.dorokhov.pony2.BlockingScanJobServiceObserver;
import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.ScanJobService;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentLibraryJobException;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistDiscoveryIndexRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryJobRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.repository.GenreRepository;
import net.dorokhov.pony2.core.library.repository.SongRepository;
import net.dorokhov.pony2.core.library.service.artwork.ArtworkStorage;
import net.dorokhov.pony2.core.library.service.artwork.command.FileArtworkStorageCommand;
import net.dorokhov.pony2.web.dto.*;
import net.dorokhov.pony2.web.dto.ErrorDto.Code;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static net.dorokhov.pony2.test.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class LibraryControllerTest extends InstallingIntegrationTest {

    private static final Resource IMAGE_RESOURCE = new ClassPathResource("image.png");

    @Autowired
    private ApiTemplate apiTemplate;

    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private SongRepository songRepository;
    @Autowired
    private ArtistDiscoveryIndexRepository artistDiscoveryIndexRepository;
    @Autowired
    private DiscoveryJobRepository discoveryJobRepository;
    @Autowired
    private DiscoveryTaskRepository discoveryTaskRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ArtworkStorage artworkStorage;

    @Autowired
    private ScanJobService scanJobService;

    private ArtworkFiles artworkFiles;

    private Genre genre1;
    private Genre genre2;

    private Artist artist1;
    private Artist artist2;

    private Album album1_1;
    private Album album1_2;
    private Album album2_1;

    private Song song1_1_1;
    private Song song1_1_2;
    private Song song1_2_1;
    private Song song2_1_1;

    private final BlockingScanJobServiceObserver blockingObserver = new BlockingScanJobServiceObserver();

    @BeforeEach
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void setUp() {
        getTransactionTemplate().execute(transactionStatus -> {

            try {
                artworkFiles = artworkStorage.getOrSave(new FileArtworkStorageCommand(IMAGE_RESOURCE.getURI(), IMAGE_RESOURCE.getFile()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            genre1 = genreRepository.save(new Genre()
                    .setArtwork(artworkFiles.getArtwork())
                    .setName("foo genre1"));
            genre2 = genreRepository.save(new Genre()
                    .setArtwork(artworkFiles.getArtwork())
                    .setName("foo genre2"));

            artist1 = new Artist()
                    .setArtwork(artworkFiles.getArtwork())
                    .setName("foo artist1");
            artist1.setGenres(List.of(new ArtistGenre()
                    .setArtist(artist1)
                    .setGenre(genre1)));
            artist1 = artistRepository.save(artist1);
            artist2 = new Artist()
                    .setArtwork(artworkFiles.getArtwork())
                    .setName("foo artist2");
            artist2.setGenres(List.of(new ArtistGenre()
                    .setArtist(artist2)
                    .setGenre(genre2)));
            artist2 = artistRepository.save(artist2);

            album1_1 = albumRepository.save(new Album()
                    .setYear(1986)
                    .setArtwork(artworkFiles.getArtwork())
                    .setName("foo album1_1")
                    .setArtist(artist1));
            album1_2 = albumRepository.save(new Album()
                    .setYear(1986)
                    .setArtwork(artworkFiles.getArtwork())
                    .setName("foo album1_2")
                    .setArtist(artist1));
            album2_1 = albumRepository.save(new Album()
                    .setYear(1986)
                    .setArtwork(artworkFiles.getArtwork())
                    .setName("foo album2_1")
                    .setArtist(artist2));

            song1_1_1 = songRepository.save(song()
                    .setCreationDate(null)
                    .setUpdateDate(null)
                    .setArtistName("bar")
                    .setArtwork(artworkFiles.getArtwork())
                    .setPath("song1_1_1")
                    .setName("foo song1_1_1")
                    .setAlbum(album1_1)
                    .setGenre(genre1));
            song1_1_1.getAlbum().getArtist(); // Pre-fetch.
            song1_1_1.getGenre(); // Pre-fetch.

            song1_1_2 = songRepository.save(song()
                    .setCreationDate(null)
                    .setUpdateDate(null)
                    .setArtistName("bar")
                    .setArtwork(artworkFiles.getArtwork())
                    .setPath("song1_1_2")
                    .setName("foo song1_1_2")
                    .setAlbum(album1_1)
                    .setGenre(genre1));
            song1_1_2.getAlbum().getArtist(); // Pre-fetch.
            song1_1_2.getGenre(); // Pre-fetch.

            song1_2_1 = songRepository.save(song()
                    .setCreationDate(null)
                    .setUpdateDate(null)
                    .setArtistName("bar")
                    .setArtwork(artworkFiles.getArtwork())
                    .setPath("song1_2_1")
                    .setName("foo song1_2_1")
                    .setAlbum(album1_2)
                    .setGenre(genre1));
            song1_2_1.getAlbum().getArtist(); // Pre-fetch.
            song1_2_1.getGenre(); // Pre-fetch.

            song2_1_1 = songRepository.save(song()
                    .setCreationDate(null)
                    .setUpdateDate(null)
                    .setArtistName("bar")
                    .setArtwork(artworkFiles.getArtwork())
                    .setPath("song2_1_1")
                    .setName("foo song2_1_1")
                    .setAlbum(album2_1)
                    .setGenre(genre2));
            song2_1_1.getAlbum().getArtist(); // Pre-fetch.
            song2_1_1.getGenre(); // Pre-fetch.

            return null;
        });
    }

    @AfterEach
    public void tearDown() {
        scanJobService.removeObserver(blockingObserver);
    }

    @Test
    public void shouldGetSongs() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        
        ResponseEntity<SongDetailsDto[]> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/songs/fetchByIds", HttpMethod.POST,
                apiTemplate.createHeaderRequest(List.of(song1_1_1.getId(), song2_1_1.getId(), 1000L), authentication.getAccessToken()),
                SongDetailsDto[].class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(list -> {
            assertThat(list).hasSize(2);
            assertThat(list[0]).satisfies(songDetailsDto -> {
                checkArtistDto(songDetailsDto.getAlbumDetails().getArtist(), artist1);
                checkAlbumDto(songDetailsDto.getAlbumDetails().getAlbum(), album1_1);
                checkSongDto(songDetailsDto.getSong(), song1_1_1);
            });
            assertThat(list[1]).satisfies(songDetailsDto -> {
                checkArtistDto(songDetailsDto.getAlbumDetails().getArtist(), artist2);
                checkAlbumDto(songDetailsDto.getAlbumDetails().getAlbum(), album2_1);
                checkSongDto(songDetailsDto.getSong(), song2_1_1);
            });
        });
    }

    @Test
    public void shouldGetArtists() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ArtistDto[]> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/artists", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ArtistDto[].class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(artists -> {
            assertThat(artists).hasSize(2);
            checkArtistDto(artists[0], artist1);
            checkArtistDto(artists[1], artist2);
        });
    }

    @Test
    public void shouldGetArtistSongs() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ArtistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/artistSongs/{artistId}", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ArtistSongsDto.class, artist1.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(artistSongs -> {
            checkArtistDto(artistSongs.getArtist(), artist1);
            assertThat(artistSongs.getArtist().getDiscoveryData()).isNotNull();
            assertThat(artistSongs.getArtist().getDiscoveryData().getCountryCode()).isNull();
            assertThat(artistSongs.getAlbumSongs()).allSatisfy(album ->
                    assertThat(album.getAlbum().getDiscoveryData().getReleaseType()).isSameAs(AlbumDiscoveryData.ReleaseType.UNKNOWN));
            assertThat(artistSongs.getAlbumSongs()).satisfies(albums -> {
                assertThat(albums).hasSize(2);
                assertThat(albums.get(0)).satisfies(album -> {
                    checkAlbumDto(album.getAlbum(), album1_1);
                    assertThat(album.getSongs()).satisfies(songs -> {
                        assertThat(songs).hasSize(2);
                        checkSongDto(songs.get(0), song1_1_1);
                        checkSongDto(songs.get(1), song1_1_2);
                    });
                });
                assertThat(albums.get(1)).satisfies(album -> {
                    checkAlbumDto(album.getAlbum(), album1_2);
                    assertThat(album.getSongs()).satisfies(songs -> {
                        assertThat(songs).hasSize(1);
                        checkSongDto(songs.getFirst(), song1_2_1);
                    });
                });
            });
        });
    }

    @Test
    public void shouldIncludeUnknownDiscoveryDataInArtistSongsResponse() {
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<String> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/artistSongs/{artistId}", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), String.class, artist1.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        JsonNode artistSongs = JsonConverter.fromJson(requireNonNull(response.getBody()), JsonNode.class);
        assertThat(artistSongs.has("artistDiscoveryData")).isFalse();
        JsonNode artistDiscoveryData = artistSongs.path("artist").path("discoveryData");
        assertThat(artistDiscoveryData.has("countryCode")).isTrue();
        assertThat(artistDiscoveryData.path("countryCode").isNull()).isTrue();
        assertThat(artistDiscoveryData.path("topSongIds").isArray()).isTrue();
        assertThat(artistDiscoveryData.path("topSongIds").size()).isZero();
        JsonNode albumSongs = artistSongs.path("albumSongs").get(0);
        assertThat(albumSongs.has("albumDiscoveryData")).isFalse();
        JsonNode albumDiscoveryData = albumSongs.path("album").path("discoveryData");
        assertThat(albumDiscoveryData.path("releaseType").asString()).isEqualTo("UNKNOWN");
        assertThat(albumDiscoveryData.has("releaseDate")).isFalse();
        assertThat(albumDiscoveryData.has("topSongIds")).isFalse();
    }

    @Test
    public void shouldGetArtistSongsWithLatestCompletedDiscoveryData() {
        artistDiscoveryIndexRepository.save(new ArtistDiscoveryIndex().setArtist(artist1).setCountryCode("DE"));
        artistDiscoveryIndexRepository.save(new ArtistDiscoveryIndex().setArtist(artist2).setCountryCode("GB"));
        DiscoveryJob job = discoveryJobRepository.save(new DiscoveryJob().setType(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.FAILED));
        LocalDateTime date = LocalDateTime.of(2026, 10, 15, 12, 0);
        String single = JsonConverter.toJson(new MusicBrainzAlbumData("single", "Single", List.of(), null, "artist", List.of("US")));
        String compilation = JsonConverter.toJson(new MusicBrainzAlbumData("compilation", "Album", List.of("Live", "Compilation"),
                "1986-10", "artist", List.of("US")));
        String ep = JsonConverter.toJson(new MusicBrainzAlbumData("ep", "EP", List.of(), "1987", "artist", List.of("US")));
        DiscoveryTask older = saveAlbumDiscoveryTask(album1_1, job, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA,
                DiscoveryTask.Status.COMPLETE, date.minusHours(1), single);
        jdbcTemplate.update("UPDATE discovery_task SET update_date = ? WHERE id = ?", Timestamp.valueOf(date.plusHours(5)), older.getId());
        saveAlbumDiscoveryTask(album1_1, job, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTask.Status.COMPLETE, date, compilation);
        saveAlbumDiscoveryTask(album1_1, job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date.plusHours(1), single);
        saveAlbumDiscoveryTask(album1_1, job, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTask.Status.FAILED, date.plusHours(2), single);
        saveAlbumDiscoveryTask(album1_1, job, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTask.Status.INTERRUPTED, date.plusHours(3), single);
        saveAlbumDiscoveryTask(album1_1, job, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTask.Status.STARTED, date.plusHours(4), single);
        saveAlbumDiscoveryTask(album1_2, job, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTask.Status.COMPLETE, date, ep);
        saveAlbumDiscoveryTask(album2_1, job, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTask.Status.COMPLETE, date.plusHours(5), single);
        DiscoveryTask topTracks = discoveryTaskRepository.save(new DiscoveryTask().setJob(job).setArtist(artist1)
                .setType(DiscoveryTaskType.SPOTIFY_TOP_TRACKS).setStatus(DiscoveryTask.Status.COMPLETE)
                .setParameter(JsonConverter.toJson(new DiscoveryTask.SpotifyTopTracksParameter(artist1.getId(), "artist-data")))
                .setResult(JsonConverter.toJson(new String[]{song1_1_2.getId(), null, "missing", song1_2_1.getId(), song1_1_1.getId()})));
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ArtistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/artistSongs/{artistId}", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ArtistSongsDto.class, artist1.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        ArtistSongsDto artistSongs = requireNonNull(response.getBody());
        assertThat(artistSongs.getArtist().getDiscoveryData().getCountryCode()).isEqualTo("DE");
        assertThat(artistSongs.getArtist().getDiscoveryData().getTopSongIds())
                .containsExactly(song1_1_2.getId(), song1_2_1.getId(), song1_1_1.getId());
        assertThat(artistSongs.getAlbumSongs()).extracting(album -> album.getAlbum().getDiscoveryData().getReleaseType())
                .containsExactly(AlbumDiscoveryData.ReleaseType.COMPILATION, AlbumDiscoveryData.ReleaseType.EP);
        assertThat(artistSongs.getAlbumSongs()).extracting(album -> album.getAlbum().getDiscoveryData().getReleaseDate())
                .containsExactly(LocalDateTime.of(1986, 10, 1, 0, 0), LocalDateTime.of(1987, 1, 1, 0, 0));

        ResponseEntity<ArtistDto[]> artistsResponse = apiTemplate.getRestTemplate().exchange(
                "/api/library/artists", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ArtistDto[].class);
        assertThat(artistsResponse.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(requireNonNull(artistsResponse.getBody())).filteredOn(artist -> artist.getId().equals(artist1.getId()))
                .singleElement().satisfies(artist -> {
                    assertThat(artist.getDiscoveryData().getCountryCode()).isEqualTo("DE");
                    assertThat(artist.getDiscoveryData().getTopSongIds())
                            .containsExactly(song1_1_2.getId(), song1_2_1.getId(), song1_1_1.getId());
                });

        ResponseEntity<SongDetailsDto[]> songsResponse = apiTemplate.getRestTemplate().exchange(
                "/api/library/songs/fetchByIds", HttpMethod.POST,
                apiTemplate.createHeaderRequest(List.of(song1_1_1.getId()), authentication.getAccessToken()), SongDetailsDto[].class);
        assertThat(songsResponse.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(requireNonNull(songsResponse.getBody())).singleElement().satisfies(song -> {
            assertThat(song.getAlbumDetails().getArtist().getDiscoveryData().getCountryCode()).isEqualTo("DE");
            assertThat(song.getAlbumDetails().getArtist().getDiscoveryData().getTopSongIds())
                    .containsExactly(song1_1_2.getId(), song1_2_1.getId(), song1_1_1.getId());
            assertThat(song.getAlbumDetails().getAlbum().getDiscoveryData().getReleaseType()).isSameAs(AlbumDiscoveryData.ReleaseType.COMPILATION);
            assertThat(song.getAlbumDetails().getAlbum().getDiscoveryData().getReleaseDate()).isEqualTo(LocalDateTime.of(1986, 10, 1, 0, 0));
        });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "null")
    public void shouldReturnUnknownWhenLatestCompletedDiscoveryResultIsEmpty(String result) {
        DiscoveryJob job = discoveryJobRepository.save(new DiscoveryJob().setType(DiscoveryType.ALBUM).setStatus(DiscoveryJob.Status.COMPLETE));
        LocalDateTime date = LocalDateTime.of(2026, 10, 15, 12, 0);
        String album = JsonConverter.toJson(new MusicBrainzAlbumData("album", "Album", List.of(), null, "artist", List.of()));
        saveAlbumDiscoveryTask(album1_1, job, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTask.Status.COMPLETE, date.minusHours(1), album);
        saveAlbumDiscoveryTask(album1_1, job, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTask.Status.COMPLETE, date, result);
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ArtistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/artistSongs/{artistId}", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ArtistSongsDto.class, artist1.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(requireNonNull(response.getBody()).getAlbumSongs()).allSatisfy(albumSongs ->
                assertThat(albumSongs.getAlbum().getDiscoveryData().getReleaseType()).isSameAs(AlbumDiscoveryData.ReleaseType.UNKNOWN));
    }

    private DiscoveryTask saveAlbumDiscoveryTask(Album album, DiscoveryJob job, DiscoveryTaskType type, DiscoveryTask.Status status,
                                                LocalDateTime creationDate, String result) {
        return getTransactionTemplate().execute(transactionStatus -> {
            DiscoveryTask task = discoveryTaskRepository.saveAndFlush(new DiscoveryTask().setJob(job).setAlbum(album).setType(type).setStatus(status)
                    .setParameter(JsonConverter.toJson(new DiscoveryTask.AlbumParameter(album.getId()))).setResult(result));
            jdbcTemplate.update("UPDATE discovery_task SET creation_date = ? WHERE id = ?", Timestamp.valueOf(creationDate), task.getId());
            return task;
        });
    }

    @Test
    public void shouldFailGettingArtistSongsIfArtistIsNotFound() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/artistSongs/1000", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("Artist");
            assertThat(error.getArguments().get(1)).isEqualTo("1000");
        });
    }

    @Test
    public void shouldGetGenres() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<GenreDto[]> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/genres", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), GenreDto[].class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(genres -> {
            assertThat(genres).hasSize(2);
            checkGenreDto(genres[0], genre1);
            checkGenreDto(genres[1], genre2);
        });
    }

    @Test
    public void shouldGetGenreSongs() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<GenreSongsPageDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/genreSongs/{genreId}", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), GenreSongsPageDto.class, genre1.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(genreSongs -> {
            assertThat(genreSongs.getPageIndex()).isEqualTo(0);
            assertThat(genreSongs.getPageSize()).isGreaterThan(0);
            assertThat(genreSongs.getTotalPages()).isEqualTo(1);
            checkGenreDto(genreSongs.getGenre(), genre1);
            assertThat(genreSongs.getSongAlbums()).satisfies(songs -> {
                assertThat(songs).hasSize(3);
                assertThat(songs.get(0)).satisfies(song -> {
                    checkSongDto(song.getSong(), song1_1_1);
                    checkAlbumDto(song.getAlbumDetails().getAlbum(), album1_1);
                    checkArtistDto(song.getAlbumDetails().getArtist(), artist1);
                });
                assertThat(songs.get(1)).satisfies(song -> {
                    checkSongDto(song.getSong(), song1_1_2);
                    checkAlbumDto(song.getAlbumDetails().getAlbum(), album1_1);
                    checkArtistDto(song.getAlbumDetails().getArtist(), artist1);
                });
                assertThat(songs.get(2)).satisfies(song -> {
                    checkSongDto(song.getSong(), song1_2_1);
                    checkAlbumDto(song.getAlbumDetails().getAlbum(), album1_2);
                    checkArtistDto(song.getAlbumDetails().getArtist(), artist1);
                });
            });
        });
    }

    @Test
    public void shouldFailGettingGenreSongsIfGenreIsNotFound() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/genreSongs/1000", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("Genre");
            assertThat(error.getArguments().get(1)).isEqualTo("1000");
        });
    }

    @Test
    public void shouldSearch() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<SearchResultDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/search?query=foo", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), SearchResultDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(searchResult -> {
            assertThat(searchResult.getGenres()).satisfies(genres -> {
                assertThat(genres).hasSize(2);
                checkGenreDto(genres.get(0), genre1);
                checkGenreDto(genres.get(1), genre2);
            });
            assertThat(searchResult.getArtists()).satisfies(artists -> {
                assertThat(artists).hasSize(2);
                checkArtistDto(artists.get(0), artist1);
                checkArtistDto(artists.get(1), artist2);
            });
            assertThat(searchResult.getAlbumDetails()).satisfies(albums -> {
                assertThat(albums).hasSize(3);
                assertThat(albums.get(0)).satisfies(album -> {
                    checkArtistDto(album.getArtist(), artist1);
                    checkAlbumDto(album.getAlbum(), album1_1);
                });
                assertThat(albums.get(1)).satisfies(album -> {
                    checkArtistDto(album.getArtist(), artist1);
                    checkAlbumDto(album.getAlbum(), album1_2);
                });
                assertThat(albums.get(2)).satisfies(album -> {
                    checkArtistDto(album.getArtist(), artist2);
                    checkAlbumDto(album.getAlbum(), album2_1);
                });
            });
            assertThat(searchResult.getSongDetails()).satisfies(songs -> {
                assertThat(songs).hasSize(4);
                assertThat(songs.get(0)).satisfies(song -> {
                    checkSongDto(song.getSong(), song1_1_1);
                    checkGenreDto(song.getGenre(), genre1);
                    checkAlbumDto(song.getAlbumDetails().getAlbum(), album1_1);
                    checkArtistDto(song.getAlbumDetails().getArtist(), artist1);
                });
                assertThat(songs.get(1)).satisfies(song -> {
                    checkSongDto(song.getSong(), song1_1_2);
                    checkGenreDto(song.getGenre(), genre1);
                    checkAlbumDto(song.getAlbumDetails().getAlbum(), album1_1);
                    checkArtistDto(song.getAlbumDetails().getArtist(), artist1);
                });
                assertThat(songs.get(2)).satisfies(song -> {
                    checkSongDto(song.getSong(), song1_2_1);
                    checkGenreDto(song.getGenre(), genre1);
                    checkAlbumDto(song.getAlbumDetails().getAlbum(), album1_2);
                    checkArtistDto(song.getAlbumDetails().getArtist(), artist1);
                });
                assertThat(songs.get(3)).satisfies(song -> {
                    checkSongDto(song.getSong(), song2_1_1);
                    checkGenreDto(song.getGenre(), genre2);
                    checkAlbumDto(song.getAlbumDetails().getAlbum(), album2_1);
                    checkArtistDto(song.getAlbumDetails().getArtist(), artist2);
                });
            });
        });
    }

    @Test
    public void shouldGetRandomSongs() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        RandomSongsRequestDto request = new RandomSongsRequestDto()
                .setCount(3);
        ResponseEntity<SongDetailsDto[]> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/fetchRandomSongs", HttpMethod.POST,
                apiTemplate.createHeaderRequest(request, authentication.getAccessToken()), SongDetailsDto[].class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(songs ->
                assertThat(songs).hasSize(3));
    }

    @Test
    public void shouldNotGetTooManyRandomSongs() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        RandomSongsRequestDto request = new RandomSongsRequestDto()
                .setCount(1000);
        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/fetchRandomSongs", HttpMethod.POST,
                apiTemplate.createHeaderRequest(request, authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void shouldGetGenreRandomSongs() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        RandomSongsRequestDto request = new RandomSongsRequestDto()
                .setCount(3)
                .setGenreIds(List.of(genre1.getId()));
        ResponseEntity<SongDetailsDto[]> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/fetchRandomSongs", HttpMethod.POST,
                apiTemplate.createHeaderRequest(request, authentication.getAccessToken()), SongDetailsDto[].class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(songs ->
                assertThat(songs).hasSize(3));
    }

    @Test
    public void shouldNotGetTooManyGenreRandomSongs() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        RandomSongsRequestDto request = new RandomSongsRequestDto()
                .setCount(1000)
                .setGenreIds(List.of(genre1.getId()));
        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/fetchRandomSongs", HttpMethod.POST,
                apiTemplate.createHeaderRequest(request, authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void shouldGetNegativeScanStatus() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ScanStatusDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/scanStatus", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ScanStatusDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(scanStatus -> 
                assertThat(scanStatus.isScanning()).isFalse());
    }

    @Test
    public void shouldGetPositiveScanStatus() throws ConcurrentLibraryJobException {

        scanJobService.addObserver(blockingObserver);
        ScanJob scanJob = scanJobService.startScanJob();
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ScanStatusDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/scanStatus", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ScanStatusDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(scanStatus ->
                assertThat(scanStatus.isScanning()).isTrue());
        
        blockingObserver.unlock();

        await().until(() -> scanJobService.getById(scanJob.getId()).orElseThrow().getStatus() == ScanJob.Status.COMPLETE);
    }

    @Test
    public void shouldGetScanStatistics() throws ConcurrentLibraryJobException {

        ScanJob scanJob = scanJobService.startScanJob();
        await().until(() -> scanJobService.getById(scanJob.getId()).orElseThrow().getStatus() == ScanJob.Status.COMPLETE);
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<OptionalResponseDto<ScanStatisticsDto>> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/scanStatistics", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(optionalResponse -> {
            assertThat(optionalResponse.isPresent()).isTrue();
            assertThat(optionalResponse.getValue()).satisfies(scanStatistics -> {
                assertThat(scanStatistics.getDate()).isNotNull();
                assertThat(scanStatistics.getDuration()).isEqualTo(0);
                assertThat(scanStatistics.getSongSize()).isEqualTo(0);
                assertThat(scanStatistics.getArtworkSize()).isEqualTo(0);
                assertThat(scanStatistics.getGenreCount()).isEqualTo(0);
                assertThat(scanStatistics.getArtistCount()).isEqualTo(0);
                assertThat(scanStatistics.getAlbumCount()).isEqualTo(0);
                assertThat(scanStatistics.getSongCount()).isEqualTo(0);
                assertThat(scanStatistics.getArtworkCount()).isEqualTo(0);
            });
        });
    }

    @Test
    public void shouldFailGettingScanStatisticsIfNotScannedYet() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<OptionalResponseDto<ScanStatisticsDto>> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/scanStatistics", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(optionalResponse -> {
            assertThat(optionalResponse.isPresent()).isFalse();
            assertThat(optionalResponse.getValue()).isNull();
        });
    }

    private void checkGenreDto(GenreDto dto, Genre genre) {
        assertThat(dto.getId()).isEqualTo(genre.getId());
        assertThat(dto.getCreationDate()).isEqualTo(genre.getCreationDate());
        assertThat(dto.getUpdateDate()).isEqualTo(genre.getUpdateDate());
        assertThat(dto.getName()).isEqualTo(genre.getName());
        assertThat(dto.getArtworkId()).isEqualTo(requireNonNull(genre.getArtwork()).getId());
    }

    private void checkArtistDto(ArtistDto dto, Artist artist) {
        assertThat(dto.getId()).isEqualTo(artist.getId());
        assertThat(dto.getCreationDate()).isEqualTo(artist.getCreationDate());
        assertThat(dto.getUpdateDate()).isEqualTo(artist.getUpdateDate());
        assertThat(dto.getName()).isEqualTo(artist.getName());
        assertThat(dto.getArtworkId()).isEqualTo(requireNonNull(artist.getArtwork()).getId());
        assertThat(dto.getDiscoveryData()).isNotNull();
        assertThat(dto.getDiscoveryData().getCountryCode()).isNull();
        assertThat(dto.getDiscoveryData().getTopSongIds()).isEmpty();
    }

    @SuppressWarnings("ConstantConditions")
    private void checkAlbumDto(AlbumDto dto, Album album) {
        assertThat(dto.getId()).isEqualTo(album.getId());
        assertThat(dto.getCreationDate()).isEqualTo(album.getCreationDate());
        assertThat(dto.getUpdateDate()).isEqualTo(album.getUpdateDate());
        assertThat(dto.getName()).isEqualTo(album.getName());
        assertThat(dto.getYear()).isEqualTo(album.getYear());
        assertThat(dto.getArtworkId()).isEqualTo(album.getArtwork().getId());
        assertThat(dto.getArtistId()).isEqualTo(album.getArtist().getId());
        assertThat(dto.getDiscoveryData()).isNotNull();
        assertThat(dto.getDiscoveryData().getReleaseType()).isSameAs(AlbumDiscoveryData.ReleaseType.UNKNOWN);
        assertThat(dto.getDiscoveryData().getReleaseDate()).isNull();
    }

    private void checkSongDto(SongDto dto, Song song) {
        assertThat(dto.getId()).isEqualTo(song.getId());
        assertThat(dto.getCreationDate()).isEqualTo(song.getCreationDate());
        assertThat(dto.getUpdateDate()).isEqualTo(song.getUpdateDate());
        assertThat(dto.getMimeType()).isEqualTo(song.getFileType().getMimeType());
        assertThat(dto.getFileExtension()).isEqualTo(song.getFileType().getFileExtension());
        assertThat(dto.getSize()).isEqualTo(song.getSize());
        assertThat(dto.getDuration()).isEqualTo(song.getDuration());
        assertThat(dto.getBitRate()).isEqualTo(song.getBitRate());
        assertThat(dto.getBitRateVariable()).isEqualTo(song.getBitRateVariable());
        assertThat(dto.getDiscNumber()).isEqualTo(1);
        assertThat(dto.getTrackNumber()).isEqualTo(song.getTrackNumber());
        assertThat(dto.getName()).isEqualTo(song.getName());
        assertThat(dto.getArtistName()).isEqualTo("bar");
        assertThat(dto.getGenreName()).isEqualTo(song.getGenreName());
        assertThat(dto.getAlbumId()).isEqualTo(song.getAlbum().getId());
        assertThat(dto.getGenreId()).isEqualTo(song.getGenre().getId());
    }
}
