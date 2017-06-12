package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.ApiTemplate;
import net.dorokhov.pony.InstallingIntegrationTest;
import net.dorokhov.pony.fixture.SongFixtures;
import net.dorokhov.pony.library.domain.*;
import net.dorokhov.pony.library.repository.AlbumRepository;
import net.dorokhov.pony.library.repository.ArtistRepository;
import net.dorokhov.pony.library.repository.GenreRepository;
import net.dorokhov.pony.library.repository.SongRepository;
import net.dorokhov.pony.library.service.impl.artwork.ArtworkStorage;
import net.dorokhov.pony.library.service.impl.artwork.command.FileArtworkStorageCommand;
import net.dorokhov.pony.library.service.impl.file.FileTypeResolver;
import net.dorokhov.pony.web.domain.UserTokenDto;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

public class FileControllerTest extends InstallingIntegrationTest {

    private static final Resource AUDIO_RESOURCE = new ClassPathResource("audio/empty.mp3");
    private static final Resource IMAGE_RESOURCE = new ClassPathResource("image.png");
    
    @Autowired
    private TestRestTemplate restTemplate;

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

    @Override
    public void setUp() throws Exception {
        super.setUp();
        artworkFiles = artworkStorage.getOrSave(new FileArtworkStorageCommand(IMAGE_RESOURCE.getURI(), IMAGE_RESOURCE.getFile()));
        Genre genre = genreRepository.save(Genre.builder().build());
        Artist artist = artistRepository.save(Artist.builder().build());
        album = albumRepository.save(Album.builder()
                .artist(artist)
                .artwork(artworkFiles.getArtwork())
                .build());
        song = songRepository.save(SongFixtures.songBuilder()
                .genre(genre)
                .album(album)
                .fileType(FileType.of("audio/mpeg", "mp3"))
                .path(AUDIO_RESOURCE.getFile().getAbsolutePath())
                .artwork(artworkFiles.getArtwork())
                .build());
    }

    @Test
    public void shouldServeAudio() throws Exception {
        byte[] expectedContents = Files.readAllBytes(AUDIO_RESOURCE.getFile().toPath());
        UserTokenDto userToken = apiTemplate.authenticateAdmin();
        ResponseEntity<byte[]> response = restTemplate.exchange("/file/audio/{id}", HttpMethod.GET, 
                apiTemplate.createCookieRequest(userToken.getToken()), byte[].class, song.getId());
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedContents);
    }

    @Test
    public void shouldServeLargeArtwork() throws Exception {
        byte[] expectedContents = Files.readAllBytes(artworkFiles.getLargeFile().toPath());
        UserTokenDto userToken = apiTemplate.authenticateAdmin();
        ResponseEntity<byte[]> response = restTemplate.exchange("/file/artwork/large/{artworkId}", HttpMethod.GET,
                apiTemplate.createCookieRequest(userToken.getToken()), byte[].class, artworkFiles.getArtwork().getId());
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedContents);
    }

    @Test
    public void shouldServeSmallArtwork() throws Exception {
        byte[] expectedContents = Files.readAllBytes(artworkFiles.getSmallFile().toPath());
        UserTokenDto userToken = apiTemplate.authenticateAdmin();
        ResponseEntity<byte[]> response = restTemplate.exchange("/file/artwork/small/{artworkId}", HttpMethod.GET,
                apiTemplate.createCookieRequest(userToken.getToken()), byte[].class, artworkFiles.getArtwork().getId());
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedContents);
    }

    @Test
    public void shouldExportSong() throws Exception {
        byte[] expectedContents = Files.readAllBytes(AUDIO_RESOURCE.getFile().toPath());
        UserTokenDto userToken = apiTemplate.authenticateAdmin();
        ResponseEntity<byte[]> response = restTemplate.exchange("/file/export/song/{songId}", HttpMethod.GET,
                apiTemplate.createCookieRequest(userToken.getToken()), byte[].class, song.getId());
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getHeaders().get("Content-Disposition")).isNotNull();
        assertThat(response.getHeaders().getFirst("Content-Type")).isEqualTo("audio/mpeg");
        assertThat(response.getBody()).isEqualTo(expectedContents);
    }

    @Test
    public void shouldExportAlbum() throws Exception {
        UserTokenDto userToken = apiTemplate.authenticateAdmin();
        ResponseEntity<byte[]> response = restTemplate.exchange("/file/export/album/{albumId}", HttpMethod.GET,
                apiTemplate.createCookieRequest(userToken.getToken()), byte[].class, album.getId());
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getHeaders().get("Content-Disposition")).isNotNull();
        assertThat(response.getHeaders().getFirst("Content-Type")).isEqualTo("application/zip");
        FileType fileType = fileTypeResolver.resolve(response.getBody());
        assertThat(fileType.getMimeType()).isEqualTo("application/zip");
    }
}
