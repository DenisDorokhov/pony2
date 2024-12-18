package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.ApiTemplate;
import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.api.user.service.command.UserCreationCommand;
import net.dorokhov.pony2.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony2.core.library.repository.*;
import net.dorokhov.pony2.web.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static net.dorokhov.pony2.test.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;

public class PlaylistControllerTest extends InstallingIntegrationTest {

    @Autowired
    private ApiTemplate apiTemplate;

    @Autowired
    private UserService userService;
    @Autowired
    private PlaylistRepository playlistRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private SongRepository songRepository;

    private AuthenticationDto authentication;
    private User user;

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

    @BeforeEach
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void setUp() {

        authentication = apiTemplate.authenticateAdmin();
        user = userService.getById(authentication.getUser().getId()).orElseThrow();

        getTransactionTemplate().executeWithoutResult(transactionStatus -> {

            genre1 = genreRepository.save(new Genre()
                    .setName("foo genre1"));
            genre2 = genreRepository.save(new Genre()
                    .setName("foo genre2"));

            artist1 = artistRepository.save(new Artist()
                    .setName("foo artist1"));
            artist2 = artistRepository.save(new Artist()
                    .setName("foo artist2"));

            album1_1 = albumRepository.save(new Album()
                    .setYear(1986)
                    .setName("foo album1_1")
                    .setArtist(artist1));
            album1_2 = albumRepository.save(new Album()
                    .setYear(1986)
                    .setName("foo album1_2")
                    .setArtist(artist1));
            album2_1 = albumRepository.save(new Album()
                    .setYear(1986)
                    .setName("foo album2_1")
                    .setArtist(artist2));

            song1_1_1 = songRepository.save(song()
                    .setId(null)
                    .setCreationDate(null)
                    .setUpdateDate(null)
                    .setArtistName("bar")
                    .setPath("song1_1_1")
                    .setName("foo song1_1_1")
                    .setAlbum(album1_1)
                    .setGenre(genre1));
            song1_1_1.getAlbum().getArtist(); // Pre-fetch.
            song1_1_1.getGenre(); // Pre-fetch.

            song1_1_2 = songRepository.save(song()
                    .setId(null)
                    .setCreationDate(null)
                    .setUpdateDate(null)
                    .setArtistName("bar")
                    .setPath("song1_1_2")
                    .setName("foo song1_1_2")
                    .setAlbum(album1_1)
                    .setGenre(genre1));
            song1_1_2.getAlbum().getArtist(); // Pre-fetch.
            song1_1_2.getGenre(); // Pre-fetch.

            song1_2_1 = songRepository.save(song()
                    .setId(null)
                    .setCreationDate(null)
                    .setUpdateDate(null)
                    .setArtistName("bar")
                    .setPath("song1_2_1")
                    .setName("foo song1_2_1")
                    .setAlbum(album1_2)
                    .setGenre(genre1));
            song1_2_1.getAlbum().getArtist(); // Pre-fetch.
            song1_2_1.getGenre(); // Pre-fetch.

            song2_1_1 = songRepository.save(song()
                    .setId(null)
                    .setCreationDate(null)
                    .setUpdateDate(null)
                    .setArtistName("bar")
                    .setPath("song2_1_1")
                    .setName("foo song2_1_1")
                    .setAlbum(album2_1)
                    .setGenre(genre2));
            song2_1_1.getAlbum().getArtist(); // Pre-fetch.
            song2_1_1.getGenre(); // Pre-fetch.
        });
    }

    @Test
    public void shouldGetAllPlaylists() {

        Playlist savedPlaylist = new Playlist()
                .setName("playlist1")
                .setType(Playlist.Type.NORMAL)
                .setUser(user);
        playlistRepository.save(savedPlaylist
                .setSongs(List.of(
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(0)
                                .setSong(song1_1_1),
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(1)
                                .setSong(song1_1_2),
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(2)
                                .setSong(song1_2_1)
                )));

        ResponseEntity<PlaylistDto[]> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaylistDto[].class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(playlists -> {
            assertThat(playlists).hasSize(3);
            assertThat(Arrays.stream(playlists).map(PlaylistDto::getType).toList())
                    .containsExactlyInAnyOrder(Playlist.Type.NORMAL, Playlist.Type.LIKE, Playlist.Type.HISTORY);
            for (PlaylistDto playlist : playlists) {
                assertThat(playlist.getCreationDate()).isNotNull();
                assertThat(playlist.getUpdateDate()).isNull();
                if (playlist.getType() == Playlist.Type.NORMAL) {
                    assertThat(playlist.getId()).isEqualTo(savedPlaylist.getId());
                    assertThat(playlist.getName()).isEqualTo("playlist1");
                }
            }
        });
    }

    @Test
    public void shouldGetAllPlaylistsByType() {

        Playlist savedPlaylist = new Playlist()
                .setName("playlist1")
                .setType(Playlist.Type.NORMAL)
                .setUser(user);
        playlistRepository.save(savedPlaylist
                .setSongs(List.of(
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(0)
                                .setSong(song1_1_1),
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(1)
                                .setSong(song1_1_2),
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(2)
                                .setSong(song1_2_1)
                )));

        ResponseEntity<PlaylistDto[]> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists?type=NORMAL", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaylistDto[].class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(playlists -> {
            assertThat(playlists).hasSize(1);
            for (PlaylistDto playlist : playlists) {
                assertThat(playlist.getId()).isEqualTo(savedPlaylist.getId());
                assertThat(playlist.getCreationDate()).isNotNull();
                assertThat(playlist.getUpdateDate()).isNull();
                assertThat(playlist.getName()).isEqualTo("playlist1");
                assertThat(playlist.getType()).isEqualTo(Playlist.Type.NORMAL);
            }
        });
    }

    @Test
    public void shouldGetPlaylistById() {

        Playlist savedPlaylist = new Playlist()
                .setName("playlist1")
                .setType(Playlist.Type.NORMAL)
                .setUser(user);
        playlistRepository.save(savedPlaylist
                .setSongs(List.of(
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(0)
                                .setSong(song1_1_1),
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(1)
                                .setSong(song1_1_2),
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(2)
                                .setSong(song1_2_1)
                )));

        ResponseEntity<PlaylistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists/{playlistId}", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaylistSongsDto.class, savedPlaylist.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(playlistSongs -> {
            assertThat(playlistSongs.getPlaylist()).satisfies(playlist -> {
                assertThat(playlist.getId()).isEqualTo(savedPlaylist.getId());
                assertThat(playlist.getCreationDate()).isNotNull();
                assertThat(playlist.getUpdateDate()).isNull();
                assertThat(playlist.getName()).isEqualTo("playlist1");
                assertThat(playlist.getType()).isEqualTo(Playlist.Type.NORMAL);
            });
            assertThat(playlistSongs.getSongs()).hasSize(3);
            assertThat(playlistSongs.getSongs().get(0)).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                checkSongDto(song.getSong(), song1_1_1);
            });
            assertThat(playlistSongs.getSongs().get(1)).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                checkSongDto(song.getSong(), song1_1_2);
            });
            assertThat(playlistSongs.getSongs().get(2)).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                checkSongDto(song.getSong(), song1_2_1);
            });
        });
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

    @Test
    public void shouldFailGettingPlaylistOfOtherUser() throws DuplicateEmailException {

        User otherUser = userService.create(new UserCreationCommand()
                .setName("Plain User")
                .setEmail("new@email.com")
                .setPassword("foobar")
                .setRoles(Set.of(User.Role.USER)));

        Playlist savedPlaylist = new Playlist()
                .setName("playlist1")
                .setType(Playlist.Type.NORMAL)
                .setUser(otherUser);
        playlistRepository.save(savedPlaylist
                .setSongs(List.of(
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(0)
                                .setSong(song1_1_1),
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(1)
                                .setSong(song1_1_2),
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(2)
                                .setSong(song1_2_1)
                )));

        ResponseEntity<PlaylistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists/{playlistId}", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaylistSongsDto.class, savedPlaylist.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
    }

    @Test
    public void shouldFailGettingNotExistingPlaylist() {

        ResponseEntity<PlaylistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists/{playlistId}", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaylistSongsDto.class, "foobar");

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
    }

    @Test
    public void shouldDeletePlaylist() {

        Playlist savedPlaylist = new Playlist()
                .setName("playlist1")
                .setType(Playlist.Type.NORMAL)
                .setUser(user);
        playlistRepository.save(savedPlaylist
                .setSongs(List.of(
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(0)
                                .setSong(song1_1_1),
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(1)
                                .setSong(song1_1_2),
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(2)
                                .setSong(song1_2_1)
                )));

        ResponseEntity<PlaylistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists/{playlistId}", HttpMethod.DELETE,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaylistSongsDto.class, savedPlaylist.getId());

        assertThat(playlistRepository.existsById(savedPlaylist.getId())).isFalse();

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(playlistSongs -> {
            assertThat(playlistSongs.getPlaylist()).satisfies(playlist -> {
                assertThat(playlist.getId()).isEqualTo(savedPlaylist.getId());
                assertThat(playlist.getCreationDate()).isNotNull();
                assertThat(playlist.getUpdateDate()).isNull();
                assertThat(playlist.getName()).isEqualTo("playlist1");
                assertThat(playlist.getType()).isEqualTo(Playlist.Type.NORMAL);
            });
            assertThat(playlistSongs.getSongs()).hasSize(3);
            assertThat(playlistSongs.getSongs().get(0)).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                checkSongDto(song.getSong(), song1_1_1);
            });
            assertThat(playlistSongs.getSongs().get(1)).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                checkSongDto(song.getSong(), song1_1_2);
            });
            assertThat(playlistSongs.getSongs().get(2)).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                checkSongDto(song.getSong(), song1_2_1);
            });
        });
    }

    @Test
    public void shouldFailDeletingPlaylistOfOtherUser() throws DuplicateEmailException {

        User otherUser = userService.create(new UserCreationCommand()
                .setName("Plain User")
                .setEmail("new@email.com")
                .setPassword("foobar")
                .setRoles(Set.of(User.Role.USER)));

        Playlist savedPlaylist = new Playlist()
                .setName("playlist1")
                .setType(Playlist.Type.NORMAL)
                .setUser(otherUser);
        playlistRepository.save(savedPlaylist
                .setSongs(List.of(
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(0)
                                .setSong(song1_1_1),
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(1)
                                .setSong(song1_1_2),
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(2)
                                .setSong(song1_2_1)
                )));

        ResponseEntity<PlaylistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists/{playlistId}", HttpMethod.DELETE,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaylistSongsDto.class, savedPlaylist.getId());

        assertThat(playlistRepository.existsById(savedPlaylist.getId())).isTrue();

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
    }

    @Test
    public void shouldFailDeletingNotExistingPlaylist() {

        ResponseEntity<PlaylistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists/{playlistId}", HttpMethod.DELETE,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaylistSongsDto.class, "foobar");

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
    }

    @Test
    public void shouldCreatePlaylist() {

        PlaylistCreationCommandDto command = new PlaylistCreationCommandDto()
                .setName("playlist1")
                .setSongIds(List.of(song1_1_1.getId(), song1_1_2.getId(), song1_2_1.getId()));

        ResponseEntity<PlaylistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists", HttpMethod.POST,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), PlaylistSongsDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(playlistSongs -> {
            assertThat(playlistSongs.getPlaylist()).satisfies(playlist -> {
                assertThat(playlist.getId()).isNotNull();
                assertThat(playlistRepository.existsById(playlist.getId())).isTrue();
                assertThat(playlist.getCreationDate()).isNotNull();
                assertThat(playlist.getUpdateDate()).isNull();
                assertThat(playlist.getName()).isEqualTo("playlist1");
                assertThat(playlist.getType()).isEqualTo(Playlist.Type.NORMAL);
            });
            assertThat(playlistSongs.getSongs()).hasSize(3);
            assertThat(playlistSongs.getSongs().get(0)).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                checkSongDto(song.getSong(), song1_1_1);
            });
            assertThat(playlistSongs.getSongs().get(1)).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                checkSongDto(song.getSong(), song1_1_2);
            });
            assertThat(playlistSongs.getSongs().get(2)).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                checkSongDto(song.getSong(), song1_2_1);
            });
        });
    }

    @Test
    public void shouldValidatePlaylistCreationCommand() {

        PlaylistCreationCommandDto command = new PlaylistCreationCommandDto();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists", HttpMethod.POST,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.VALIDATION);
            assertThat(error.getFieldViolations().size()).isGreaterThanOrEqualTo(2);
            assertThat(error.getFieldViolations().stream()
                    .map(ErrorDto.FieldViolation::getField).distinct())
                    .containsExactlyInAnyOrder("name", "songIds");
        });
    }

    @Test
    public void shouldUpdatePlaylist() {

        Playlist savedPlaylist = new Playlist()
                .setName("playlist1")
                .setType(Playlist.Type.NORMAL)
                .setUser(user);
        playlistRepository.save(savedPlaylist
                .setSongs(List.of(
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(0)
                                .setSong(song1_2_1)
                )));

        String idSong1_2_1 = savedPlaylist.getSongs().getFirst().getId();

        PlaylistUpdateCommandDto command = new PlaylistUpdateCommandDto()
                .setId(savedPlaylist.getId())
                .setName("playlist2")
                .setSongIds(List.of(
                        new PlaylistUpdateCommandDto.PlaylistSongId()
                                .setSongId(song1_1_1.getId()),
                        new PlaylistUpdateCommandDto.PlaylistSongId()
                                .setSongId(song1_1_2.getId()),
                        new PlaylistUpdateCommandDto.PlaylistSongId()
                                .setId(idSong1_2_1)
                                .setSongId(song1_2_1.getId())
                ));

        ResponseEntity<PlaylistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists", HttpMethod.PUT,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), PlaylistSongsDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(playlistSongs -> {
            assertThat(playlistSongs.getPlaylist()).satisfies(playlist -> {
                assertThat(playlist.getId()).isNotNull();
                assertThat(playlistRepository.existsById(playlist.getId())).isTrue();
                assertThat(playlist.getCreationDate()).isNotNull();
                assertThat(playlist.getUpdateDate()).isNotNull();
                assertThat(playlist.getName()).isEqualTo("playlist2");
                assertThat(playlist.getType()).isEqualTo(Playlist.Type.NORMAL);
            });
            assertThat(playlistSongs.getSongs()).hasSize(3);
            assertThat(playlistSongs.getSongs().get(0)).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                checkSongDto(song.getSong(), song1_1_1);
            });
            assertThat(playlistSongs.getSongs().get(1)).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                checkSongDto(song.getSong(), song1_1_2);
            });
            assertThat(playlistSongs.getSongs().get(2)).satisfies(song -> {
                assertThat(song.getId()).isEqualTo(idSong1_2_1);
                checkSongDto(song.getSong(), song1_2_1);
            });
        });
    }

    @Test
    public void shouldValidatePlaylistUpdateCommand() {

        Playlist savedPlaylist = new Playlist()
                .setName("playlist1")
                .setType(Playlist.Type.NORMAL)
                .setUser(user);
        playlistRepository.save(savedPlaylist
                .setSongs(List.of(
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(0)
                                .setSong(song1_2_1)
                )));

        PlaylistUpdateCommandDto command = new PlaylistUpdateCommandDto();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists", HttpMethod.PUT,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.VALIDATION);
            assertThat(error.getFieldViolations().size()).isGreaterThanOrEqualTo(3);
            assertThat(error.getFieldViolations().stream()
                    .map(ErrorDto.FieldViolation::getField).distinct())
                    .containsExactlyInAnyOrder("id", "name", "songIds");
        });
    }

    @Test
    public void shouldFailUpdatingPlaylistOfOtherUser() throws DuplicateEmailException {

        User otherUser = userService.create(new UserCreationCommand()
                .setName("Plain User")
                .setEmail("new@email.com")
                .setPassword("foobar")
                .setRoles(Set.of(User.Role.USER)));

        Playlist savedPlaylist = new Playlist()
                .setName("playlist1")
                .setType(Playlist.Type.NORMAL)
                .setUser(otherUser);
        playlistRepository.save(savedPlaylist
                .setSongs(List.of(
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(0)
                                .setSong(song1_2_1)
                )));

        PlaylistUpdateCommandDto command = new PlaylistUpdateCommandDto()
                .setId(savedPlaylist.getId())
                .setName("playlist2")
                .setSongIds(List.of(
                        new PlaylistUpdateCommandDto.PlaylistSongId()
                                .setSongId(song1_1_1.getId()),
                        new PlaylistUpdateCommandDto.PlaylistSongId()
                                .setSongId(song1_1_2.getId())
                ));

        ResponseEntity<PlaylistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists", HttpMethod.PUT,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), PlaylistSongsDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
    }

    @Test
    public void shouldFailUpdatingNotExistingPlaylist() {

        PlaylistUpdateCommandDto command = new PlaylistUpdateCommandDto()
                .setId("foobar")
                .setName("playlist2")
                .setSongIds(List.of(
                        new PlaylistUpdateCommandDto.PlaylistSongId()
                                .setSongId(song1_1_1.getId()),
                        new PlaylistUpdateCommandDto.PlaylistSongId()
                                .setSongId(song1_1_2.getId())
                ));

        ResponseEntity<PlaylistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists", HttpMethod.PUT,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), PlaylistSongsDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
    }

    @Test
    public void shouldAddSongToPlaylist() {

        Playlist savedPlaylist = new Playlist()
                .setName("playlist1")
                .setType(Playlist.Type.NORMAL)
                .setUser(user);
        playlistRepository.save(savedPlaylist
                .setSongs(List.of(
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(0)
                                .setSong(song1_1_1)
                )));

        ResponseEntity<PlaylistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists/{playlistId}/addSong/{songId}", HttpMethod.POST,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaylistSongsDto.class, savedPlaylist.getId(), song1_1_2.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(playlistSongs -> {
            assertThat(playlistSongs.getPlaylist()).satisfies(playlist -> {
                assertThat(playlist.getId()).isNotNull();
                assertThat(playlistRepository.existsById(playlist.getId())).isTrue();
                assertThat(playlist.getCreationDate()).isNotNull();
                assertThat(playlist.getUpdateDate()).isNotNull();
                assertThat(playlist.getName()).isEqualTo("playlist1");
                assertThat(playlist.getType()).isEqualTo(Playlist.Type.NORMAL);
            });
            assertThat(playlistSongs.getSongs()).hasSize(2);
            assertThat(playlistSongs.getSongs().get(0)).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                checkSongDto(song.getSong(), song1_1_1);
            });
            assertThat(playlistSongs.getSongs().get(1)).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                checkSongDto(song.getSong(), song1_1_2);
            });
        });
    }

    @Test
    public void shouldFailAddingSongToPlaylistOfOtherUser() throws DuplicateEmailException {

        User otherUser = userService.create(new UserCreationCommand()
                .setName("Plain User")
                .setEmail("new@email.com")
                .setPassword("foobar")
                .setRoles(Set.of(User.Role.USER)));

        Playlist savedPlaylist = new Playlist()
                .setName("playlist1")
                .setType(Playlist.Type.NORMAL)
                .setUser(otherUser);
        playlistRepository.save(savedPlaylist
                .setSongs(List.of(
                        new PlaylistSong()
                                .setPlaylist(savedPlaylist)
                                .setSort(0)
                                .setSong(song1_2_1)
                )));

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists/{playlistId}/addSong/{songId}", HttpMethod.POST,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ErrorDto.class, savedPlaylist.getId(), song1_1_2.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
    }

    @Test
    public void shouldFailAddingSongToNotExistingPlaylist() {

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists/{playlistId}/addSong/{songId}", HttpMethod.POST,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ErrorDto.class, "foobar", song1_1_2.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
    }

    @Test
    public void shouldLikeSong() {

        ResponseEntity<PlaylistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists/likeSong/{songId}", HttpMethod.POST,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaylistSongsDto.class, song1_1_1.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(playlistSongs -> {
            assertThat(playlistSongs.getPlaylist()).satisfies(playlist -> {
                assertThat(playlist.getId()).isNotNull();
                assertThat(playlistRepository.existsById(playlist.getId())).isTrue();
                assertThat(playlist.getCreationDate()).isNotNull();
                assertThat(playlist.getUpdateDate()).isNotNull();
                assertThat(playlist.getName()).isNotNull();
                assertThat(playlist.getType()).isEqualTo(Playlist.Type.LIKE);
            });
            assertThat(playlistSongs.getSongs()).hasSize(1);
            assertThat(playlistSongs.getSongs().getFirst()).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                checkSongDto(song.getSong(), song1_1_1);
            });
        });
    }

    @Test
    public void shouldFailLikingNotExistingSong() {

        ResponseEntity<PlaylistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists/likeSong/{songId}", HttpMethod.POST,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaylistSongsDto.class, "foobar");

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
    }

    @Test
    public void shouldUnlikeSong() {

        ResponseEntity<PlaylistSongsDto> response;

        response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists/likeSong/{songId}", HttpMethod.POST,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaylistSongsDto.class, song1_1_1.getId());
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);

        response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists/likeSong/{songId}", HttpMethod.DELETE,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaylistSongsDto.class, song1_1_1.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(playlistSongs -> {
            assertThat(playlistSongs.getPlaylist()).satisfies(playlist -> {
                assertThat(playlist.getId()).isNotNull();
                assertThat(playlistRepository.existsById(playlist.getId())).isTrue();
                assertThat(playlist.getCreationDate()).isNotNull();
                assertThat(playlist.getUpdateDate()).isNotNull();
                assertThat(playlist.getName()).isNotNull();
                assertThat(playlist.getType()).isEqualTo(Playlist.Type.LIKE);
            });
            assertThat(playlistSongs.getSongs()).isEmpty();
        });
    }

    @Test
    public void shouldAddSongToHistory() {

        ResponseEntity<PlaylistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists/addSongToHistory/{songId}", HttpMethod.POST,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaylistSongsDto.class, song1_1_1.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(playlistSongs -> {
            assertThat(playlistSongs.getPlaylist()).satisfies(playlist -> {
                assertThat(playlist.getId()).isNotNull();
                assertThat(playlistRepository.existsById(playlist.getId())).isTrue();
                assertThat(playlist.getCreationDate()).isNotNull();
                assertThat(playlist.getUpdateDate()).isNotNull();
                assertThat(playlist.getName()).isNotNull();
                assertThat(playlist.getType()).isEqualTo(Playlist.Type.HISTORY);
            });
            assertThat(playlistSongs.getSongs()).hasSize(1);
            assertThat(playlistSongs.getSongs().getFirst()).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                checkSongDto(song.getSong(), song1_1_1);
            });
        });
    }

    @Test
    public void shouldFailAddingNotExistingSongToHistory() {

        ResponseEntity<PlaylistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/playlists/addSongToHistory/{songId}", HttpMethod.POST,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaylistSongsDto.class, "foobar");

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
    }
}
