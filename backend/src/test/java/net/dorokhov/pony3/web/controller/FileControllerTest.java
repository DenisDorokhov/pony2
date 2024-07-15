package net.dorokhov.pony3.web.controller;

import net.dorokhov.pony3.ApiTemplate;
import net.dorokhov.pony3.InstallingIntegrationTest;
import net.dorokhov.pony3.api.library.domain.*;
import net.dorokhov.pony3.core.library.repository.AlbumRepository;
import net.dorokhov.pony3.core.library.repository.ArtistRepository;
import net.dorokhov.pony3.core.library.repository.GenreRepository;
import net.dorokhov.pony3.core.library.repository.SongRepository;
import net.dorokhov.pony3.core.library.service.artwork.ArtworkStorage;
import net.dorokhov.pony3.core.library.service.artwork.command.FileArtworkStorageCommand;
import net.dorokhov.pony3.core.library.service.file.FileTypeResolver;
import net.dorokhov.pony3.test.SongFixtures;
import net.dorokhov.pony3.web.dto.AuthenticationDto;
import net.dorokhov.pony3.web.dto.ErrorDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class FileControllerTest extends InstallingIntegrationTest {

    private static final Resource AUDIO_RESOURCE = new ClassPathResource("empty.mp3");
    private static final Resource IMAGE_RESOURCE = new ClassPathResource("image.png");

    @Autowired
    private ApiTemplate apiTemplate;
    
    @Autowired
    private ArtworkStorage artworkStorage;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private SongRepository songRepository;
    @Autowired
    private FileTypeResolver fileTypeResolver;
    
    private Album album;
    private Song song;
    private ArtworkFiles artworkFiles;
    private File songFile;

    @BeforeEach
    public void setUp() throws Exception {
        artworkFiles = artworkStorage.getOrSave(new FileArtworkStorageCommand(IMAGE_RESOURCE.getURI(), IMAGE_RESOURCE.getFile()));
        Genre genre = genreRepository.save(new Genre());
        Artist artist = artistRepository.save(new Artist());
        album = albumRepository.save(new Album()
                .setArtist(artist)
                .setArtwork(artworkFiles.getArtwork()));
        songFile = Files.createFile(tempFolder.resolve(requireNonNull(AUDIO_RESOURCE.getFilename()))).toFile();
        try (FileOutputStream fileOutputStream = new FileOutputStream(songFile)) {
            Files.copy(AUDIO_RESOURCE.getFile().toPath(), fileOutputStream);
        }
        song = songRepository.save(SongFixtures.song()
                .setGenre(genre)
                .setAlbum(album)
                .setFileType(FileType.of("audio/mpeg", "mp3"))
                .setPath(songFile.getAbsolutePath())
                .setArtwork(artworkFiles.getArtwork()));
    }

    @Test
    public void shouldServeAudio() throws IOException {

        byte[] expectedContents = Files.readAllBytes(songFile.toPath());
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<byte[]> response = apiTemplate.getRestTemplate().exchange("/api/file/audio/{id}", HttpMethod.GET, 
                apiTemplate.createCookieRequest(authentication.getStaticToken()), byte[].class, song.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedContents);
    }

    @Test
    public void shouldServeByteRangeAudio() throws IOException {

        byte[] contents = Files.readAllBytes(songFile.toPath());
        byte[] expectedContents = Arrays.copyOfRange(contents, 1024, 2048);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Range", "bytes=1024-2047");
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<byte[]> response = apiTemplate.getRestTemplate().exchange(
                "/api/file/audio/{id}", HttpMethod.GET,
                apiTemplate.createCookieRequest(null, authentication.getStaticToken(), httpHeaders), 
                byte[].class, song.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getBody()).isEqualTo(expectedContents);
    }

    @Test
    public void shouldReportNotFoundAudioForNotFoundSong() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange("/api/file/audio/1000", HttpMethod.GET,
                apiTemplate.createCookieRequest(authentication.getStaticToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("Song");
            assertThat(error.getArguments().get(1)).isEqualTo("1000");
        });
    }

    @Test
    public void shouldReportNotFoundAudioForNotFoundSongFile() {

        assertThat(songFile.delete()).isTrue();

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange("/api/file/audio/{id}", HttpMethod.GET,
                apiTemplate.createCookieRequest(authentication.getStaticToken()), ErrorDto.class, song.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("Song");
            assertThat(error.getArguments().get(1)).isEqualTo(song.getId());
        });
    }

    @Test
    public void shouldServeLargeArtwork() throws IOException {

        byte[] expectedContents = Files.readAllBytes(artworkFiles.getLargeFile().toPath());
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<byte[]> response = apiTemplate.getRestTemplate().exchange("/api/file/artwork/large/{artworkId}", HttpMethod.GET,
                apiTemplate.createCookieRequest(authentication.getStaticToken()), byte[].class, artworkFiles.getArtwork().getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedContents);
    }

    @Test
    public void shouldReportNotFoundLargeArtwork() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange("/api/file/artwork/large/1000", HttpMethod.GET,
                apiTemplate.createCookieRequest(authentication.getStaticToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("Artwork");
            assertThat(error.getArguments().get(1)).isEqualTo("1000");
        });
    }

    @Test
    public void shouldServeSmallArtwork() throws IOException {

        byte[] expectedContents = Files.readAllBytes(artworkFiles.getSmallFile().toPath());
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<byte[]> response = apiTemplate.getRestTemplate().exchange("/api/file/artwork/small/{artworkId}", HttpMethod.GET,
                apiTemplate.createCookieRequest(authentication.getStaticToken()), byte[].class, artworkFiles.getArtwork().getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedContents);
    }

    @Test
    public void shouldReportNotFoundSmallArtwork() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange("/api/file/artwork/small/1000", HttpMethod.GET,
                apiTemplate.createCookieRequest(authentication.getStaticToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("Artwork");
            assertThat(error.getArguments().get(1)).isEqualTo("1000");
        });
    }

    @Test
    public void shouldExportSong() throws IOException {

        byte[] expectedContents = Files.readAllBytes(songFile.toPath());
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<byte[]> response = apiTemplate.getRestTemplate().exchange("/api/file/export/song/{songId}", HttpMethod.GET,
                apiTemplate.createCookieRequest(authentication.getStaticToken()), byte[].class, song.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getHeaders().get("Content-Disposition")).isNotNull();
        assertThat(response.getHeaders().getFirst("Content-Type")).isEqualTo("audio/mpeg");
        assertThat(response.getBody()).isEqualTo(expectedContents);
    }

    @Test
    public void shouldReportNotFoundExportingSongForNotFoundSong() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange("/api/file/export/song/1000", HttpMethod.GET,
                apiTemplate.createCookieRequest(authentication.getStaticToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("Song");
            assertThat(error.getArguments().get(1)).isEqualTo("1000");
        });
    }

    @Test
    public void shouldReportNotFoundExportingSongForNotFoundSongFile() {
        
        assertThat(songFile.delete()).isTrue();

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange("/api/file/export/song/{songId}", HttpMethod.GET,
                apiTemplate.createCookieRequest(authentication.getStaticToken()), ErrorDto.class, song.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("Song");
            assertThat(error.getArguments().get(1)).isEqualTo(song.getId());
        });
    }

    @Test
    public void shouldExportAlbum() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<byte[]> response = apiTemplate.getRestTemplate().exchange("/api/file/export/album/{albumId}", HttpMethod.GET,
                apiTemplate.createCookieRequest(authentication.getStaticToken()), byte[].class, album.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getHeaders().get("Content-Disposition")).isNotNull();
        assertThat(response.getHeaders().getFirst("Content-Type")).isEqualTo("application/zip");
        assertThat(fileTypeResolver.resolve(response.getBody()).getMimeType()).isEqualTo("application/zip");
    }

    @Test
    public void shouldReportNotFoundExportingAlbum() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange("/api/file/export/album/1000", HttpMethod.GET,
                apiTemplate.createCookieRequest(authentication.getStaticToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("Album");
            assertThat(error.getArguments().get(1)).isEqualTo("1000");
        });
    }
}
