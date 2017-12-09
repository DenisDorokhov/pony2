package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.ApiTemplate;
import net.dorokhov.pony.InstallingIntegrationTest;
import net.dorokhov.pony.fixture.BlockingScanJobServiceObserver;
import net.dorokhov.pony.fixture.SongFixtures;
import net.dorokhov.pony.library.domain.*;
import net.dorokhov.pony.library.repository.AlbumRepository;
import net.dorokhov.pony.library.repository.ArtistRepository;
import net.dorokhov.pony.library.repository.GenreRepository;
import net.dorokhov.pony.library.repository.SongRepository;
import net.dorokhov.pony.library.service.ScanJobService;
import net.dorokhov.pony.library.service.artwork.ArtworkStorage;
import net.dorokhov.pony.library.service.artwork.command.FileArtworkStorageCommand;
import net.dorokhov.pony.web.domain.*;
import net.dorokhov.pony.web.domain.ErrorDto.Code;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

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

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getTransactionTemplate().execute(status -> {

            try {
                artworkFiles = artworkStorage.getOrSave(new FileArtworkStorageCommand(IMAGE_RESOURCE.getURI(), IMAGE_RESOURCE.getFile()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Genre.Builder genreBuilder = Genre.builder()
                    .artwork(artworkFiles.getArtwork());

            genre1 = genreRepository.save(genreBuilder
                    .name("foo genre1")
                    .build());
            genre2 = genreRepository.save(genreBuilder
                    .name("foo genre2")
                    .build());

            Artist.Builder artistBuilder = Artist.builder()
                    .artwork(artworkFiles.getArtwork());

            artist1 = artistRepository.save(artistBuilder
                    .name("foo artist1")
                    .build());
            artist2 = artistRepository.save(artistBuilder
                    .name("foo artist2")
                    .build());

            Album.Builder albumBuilder = Album.builder()
                    .year(1986)
                    .artwork(artworkFiles.getArtwork());

            album1_1 = albumRepository.save(albumBuilder
                    .name("foo album1_1")
                    .artist(artist1)
                    .build());
            album1_2 = albumRepository.save(albumBuilder
                    .name("foo album1_2")
                    .artist(artist1)
                    .build());
            album2_1 = albumRepository.save(albumBuilder
                    .name("foo album2_1")
                    .artist(artist2)
                    .build());

            Song.Builder songBuilder = SongFixtures.songBuilder()
                    .id(null)
                    .creationDate(null)
                    .updateDate(null)
                    .artistName("bar")
                    .artwork(artworkFiles.getArtwork());

            song1_1_1 = songRepository.save(songBuilder
                    .path("song1_1_1")
                    .name("foo song1_1_1")
                    .album(album1_1)
                    .genre(genre1)
                    .build());
            song1_1_1.getAlbum().getArtist();
            song1_1_1.getGenre();

            song1_1_2 = songRepository.save(songBuilder
                    .path("song1_1_2")
                    .name("foo song1_1_2")
                    .album(album1_1)
                    .genre(genre1)
                    .build());
            song1_1_2.getAlbum().getArtist();
            song1_1_2.getGenre();

            song1_2_1 = songRepository.save(songBuilder
                    .path("song1_2_1")
                    .name("foo song1_2_1")
                    .album(album1_2)
                    .genre(genre1)
                    .build());
            song1_2_1.getAlbum().getArtist();
            song1_2_1.getGenre();

            song2_1_1 = songRepository.save(songBuilder
                    .path("song2_1_1")
                    .name("foo song2_1_1")
                    .album(album2_1)
                    .genre(genre2)
                    .build());
            song2_1_1.getAlbum().getArtist();
            song2_1_1.getGenre();

            return null;
        });
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        scanJobService.removeObserver(blockingObserver);
    }

    @Test
    public void shouldGetArtists() throws Exception {
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<ArtistDto[]> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/artists", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), ArtistDto[].class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(artists -> {
            assertThat(artists).hasSize(2);
            checkArtistDto(artists[0], artist1);
            checkArtistDto(artists[1], artist2);
        });
    }

    @Test
    public void shouldGetArtistSongs() throws Exception {
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<ArtistSongsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/artistSongs/{artistId}", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), ArtistSongsDto.class, artist1.getId());
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(artistSongs -> {
            checkArtistDto(artistSongs.getArtist(), artist1);
            assertThat(artistSongs.getAlbums()).satisfies(albums -> {
                //noinspection unchecked
                assertThat(albums).hasSize(2);
                assertThat(albums.get(0)).satisfies(album -> {
                    checkAlbumDto(album.getAlbum(), album1_1);
                    assertThat(album.getSongs()).satisfies(songs -> {
                        //noinspection unchecked
                        assertThat(songs).hasSize(2);
                        checkSongDto(songs.get(0), song1_1_1);
                        checkSongDto(songs.get(1), song1_1_2);
                    });
                });
                assertThat(albums.get(1)).satisfies(album -> {
                    checkAlbumDto(album.getAlbum(), album1_2);
                    assertThat(album.getSongs()).satisfies(songs -> {
                        //noinspection unchecked
                        assertThat(songs).hasSize(1);
                        checkSongDto(songs.get(0), song1_2_1);
                    });
                });
            });
        });
    }

    @Test
    public void shouldFailGettingArtistSongsIfArtistIsNotFound() throws Exception {
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/artistSongs/1000", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("Artist");
            assertThat(error.getArguments().get(1)).isEqualTo("1000");
        });
    }

    @Test
    public void shouldGetGenres() throws Exception {
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<GenreDto[]> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/genres", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), GenreDto[].class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(genres -> {
            assertThat(genres).hasSize(2);
            checkGenreDto(genres[0], genre1);
            checkGenreDto(genres[1], genre2);
        });
    }

    @Test
    public void shouldGetGenreSongs() throws Exception {
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<GenreSongsPageDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/genreSongs/{genreId}", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), GenreSongsPageDto.class, genre1.getId());
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(genreSongs -> {
            assertThat(genreSongs.getPageIndex()).isEqualTo(0);
            assertThat(genreSongs.getPageSize()).isGreaterThan(0);
            assertThat(genreSongs.getTotalPages()).isEqualTo(1);
            checkGenreDto(genreSongs.getGenre(), genre1);
            assertThat(genreSongs.getSongs()).satisfies(songs -> {
                //noinspection unchecked
                assertThat(songs).hasSize(3);
                assertThat(songs.get(0)).satisfies(song -> {
                    checkSongDto(song.getSong(), song1_1_1);
                    checkAlbumDto(song.getAlbum().getAlbum(), album1_1);
                    checkArtistDto(song.getAlbum().getArtist(), artist1);
                });
                assertThat(songs.get(1)).satisfies(song -> {
                    checkSongDto(song.getSong(), song1_1_2);
                    checkAlbumDto(song.getAlbum().getAlbum(), album1_1);
                    checkArtistDto(song.getAlbum().getArtist(), artist1);
                });
                assertThat(songs.get(2)).satisfies(song -> {
                    checkSongDto(song.getSong(), song1_2_1);
                    checkAlbumDto(song.getAlbum().getAlbum(), album1_2);
                    checkArtistDto(song.getAlbum().getArtist(), artist1);
                });
            });
        });
    }

    @Test
    public void shouldFailGettingGenreSongsIfGenreIsNotFound() throws Exception {
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/genreSongs/1000", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("Genre");
            assertThat(error.getArguments().get(1)).isEqualTo("1000");
        });
    }

    @Test
    public void shouldSearch() throws Exception {
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<SearchResultDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/search?query=foo", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), SearchResultDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(searchResult -> {
            assertThat(searchResult.getGenres()).satisfies(genres -> {
                //noinspection unchecked
                assertThat(genres).hasSize(2);
                checkGenreDto(genres.get(0), genre1);
                checkGenreDto(genres.get(1), genre2);
            });
            assertThat(searchResult.getArtists()).satisfies(artists -> {
                //noinspection unchecked
                assertThat(artists).hasSize(2);
                checkArtistDto(artists.get(0), artist1);
                checkArtistDto(artists.get(1), artist2);
            });
            assertThat(searchResult.getAlbums()).satisfies(albums -> {
                //noinspection unchecked
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
            assertThat(searchResult.getSongs()).satisfies(songs -> {
                //noinspection unchecked
                assertThat(songs).hasSize(4);
                assertThat(songs.get(0)).satisfies(song -> {
                    checkSongDto(song.getSong(), song1_1_1);
                    checkGenreDto(song.getGenre(), genre1);
                    checkAlbumDto(song.getAlbum().getAlbum(), album1_1);
                    checkArtistDto(song.getAlbum().getArtist(), artist1);
                });
                assertThat(songs.get(1)).satisfies(song -> {
                    checkSongDto(song.getSong(), song1_1_2);
                    checkGenreDto(song.getGenre(), genre1);
                    checkAlbumDto(song.getAlbum().getAlbum(), album1_1);
                    checkArtistDto(song.getAlbum().getArtist(), artist1);
                });
                assertThat(songs.get(2)).satisfies(song -> {
                    checkSongDto(song.getSong(), song1_2_1);
                    checkGenreDto(song.getGenre(), genre1);
                    checkAlbumDto(song.getAlbum().getAlbum(), album1_2);
                    checkArtistDto(song.getAlbum().getArtist(), artist1);
                });
                assertThat(songs.get(3)).satisfies(song -> {
                    checkSongDto(song.getSong(), song2_1_1);
                    checkGenreDto(song.getGenre(), genre2);
                    checkAlbumDto(song.getAlbum().getAlbum(), album2_1);
                    checkArtistDto(song.getAlbum().getArtist(), artist2);
                });
            });
        });
    }

    @Test
    public void shouldGetNegativeScanStatus() throws Exception {
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<ScanStatusDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/scanStatus", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), ScanStatusDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(scanStatus -> 
                assertThat(scanStatus.isScanning()).isFalse());
    }

    @Test
    public void shouldGetPositiveScanStatus() throws Exception {

        scanJobService.addObserver(blockingObserver);
        ScanJob scanJob = scanJobService.startScanJob();

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<ScanStatusDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/scanStatus", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), ScanStatusDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(scanStatus ->
                assertThat(scanStatus.isScanning()).isTrue());
        
        blockingObserver.unlock();

        await().until(() -> scanJobService.getById(scanJob.getId()).getStatus() == ScanJob.Status.COMPLETE);
    }

    @Test
    public void shouldGetScanStatistics() throws Exception {
        ScanJob scanJob = scanJobService.startScanJob();
        await().until(() -> scanJobService.getById(scanJob.getId()).getStatus() == ScanJob.Status.COMPLETE);
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<ScanStatisticsDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/scanStatistics", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), ScanStatisticsDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(scanStatistics -> {
            assertThat(scanStatistics.getDuration()).isGreaterThan(0);
            assertThat(scanStatistics.getSongSize()).isEqualTo(0);
            assertThat(scanStatistics.getArtworkSize()).isEqualTo(0);
            assertThat(scanStatistics.getGenreCount()).isEqualTo(0);
            assertThat(scanStatistics.getArtistCount()).isEqualTo(0);
            assertThat(scanStatistics.getAlbumCount()).isEqualTo(0);
            assertThat(scanStatistics.getSongCount()).isEqualTo(0);
            assertThat(scanStatistics.getArtworkCount()).isEqualTo(0);
        });
    }

    @Test
    public void shouldFailGettingScanStatisticsIfNotScannedYet() throws Exception {
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/library/scanStatistics", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("ScanResult");
        });
    }

    @SuppressWarnings({"ConstantConditions", "Duplicates"})
    private void checkGenreDto(GenreDto dto, Genre genre) {
        assertThat(dto.getId()).isEqualTo(genre.getId());
        assertThat(dto.getCreationDate()).isEqualTo(genre.getCreationDate());
        assertThat(dto.getUpdateDate()).isEqualTo(genre.getUpdateDate());
        assertThat(dto.getName()).isEqualTo(genre.getName());
        assertThat(dto.getArtwork()).isEqualTo(genre.getArtwork().getId());
    }

    @SuppressWarnings({"ConstantConditions", "Duplicates"})
    private void checkArtistDto(ArtistDto dto, Artist artist) {
        assertThat(dto.getId()).isEqualTo(artist.getId());
        assertThat(dto.getCreationDate()).isEqualTo(artist.getCreationDate());
        assertThat(dto.getUpdateDate()).isEqualTo(artist.getUpdateDate());
        assertThat(dto.getName()).isEqualTo(artist.getName());
        assertThat(dto.getArtwork()).isEqualTo(artist.getArtwork().getId());
    }

    @SuppressWarnings("ConstantConditions")
    private void checkAlbumDto(AlbumDto dto, Album album) {
        assertThat(dto.getId()).isEqualTo(album.getId());
        assertThat(dto.getCreationDate()).isEqualTo(album.getCreationDate());
        assertThat(dto.getUpdateDate()).isEqualTo(album.getUpdateDate());
        assertThat(dto.getName()).isEqualTo(album.getName());
        assertThat(dto.getYear()).isEqualTo(album.getYear());
        assertThat(dto.getArtwork()).isEqualTo(album.getArtwork().getId());
        assertThat(dto.getArtist()).isEqualTo(album.getArtist().getId());
    }

    private void checkSongDto(SongDto dto, Song song) {
        assertThat(dto.getId()).isEqualTo(song.getId());
        assertThat(dto.getCreationDate()).isEqualTo(song.getCreationDate());
        assertThat(dto.getUpdateDate()).isEqualTo(song.getUpdateDate());
        assertThat(dto.getMimeType()).isEqualTo(song.getFileType().getMimeType());
        assertThat(dto.getSize()).isEqualTo(song.getSize());
        assertThat(dto.getDuration()).isEqualTo(song.getDuration());
        assertThat(dto.getBitRate()).isEqualTo(song.getBitRate());
        assertThat(dto.isBitRateVariable()).isEqualTo(song.isBitRateVariable());
        assertThat(dto.getDiscNumber()).isEqualTo(1);
        assertThat(dto.getTrackNumber()).isEqualTo(song.getTrackNumber());
        assertThat(dto.getName()).isEqualTo(song.getName());
        assertThat(dto.getArtistName()).isEqualTo("bar");
        assertThat(dto.getAlbum()).isEqualTo(song.getAlbum().getId());
        assertThat(dto.getGenre()).isEqualTo(song.getGenre().getId());
    }
}
