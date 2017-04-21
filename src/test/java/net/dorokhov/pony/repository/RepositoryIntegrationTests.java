package net.dorokhov.pony.repository;

import net.dorokhov.pony.entity.*;
import net.dorokhov.pony.test.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryIntegrationTests extends IntegrationTest {

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private ConfigRepository configRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private InstallationRepository installationRepository;

    @Autowired
    private LogMessageRepository logMessageRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void saveAlbum() throws Exception {
        Artist artist = buildArtist();
        artistRepository.save(artist);
        doTestSave(buildAlbum(artist), albumRepository);
    }

    @Test
    public void saveArtist() throws Exception {
        doTestSave(buildArtist(), artistRepository);
    }

    @Test
    public void saveConfig() throws Exception {
        doTestSave(buildConfig(), configRepository);
    }

    @Test
    public void saveGenre() throws Exception {
        doTestSave(buildGenre(), genreRepository);
    }

    @Test
    public void saveInstallation() throws Exception {
        doTestSave(buildInstallation(), installationRepository);
    }

    @Test
    public void saveLogMessage() throws Exception {
        doTestSave(buildLogMessage(), logMessageRepository);
    }

    @Test
    public void saveSong() throws Exception {
        Artist artist = buildArtist();
        artistRepository.save(artist);
        Album album = buildAlbum(artist);
        albumRepository.save(album);
        Genre genre = buildGenre();
        genreRepository.save(genre);
        doTestSave(buildSong(album, genre), songRepository);
    }

    @Test
    public void saveArtwork() throws Exception {
        doTestSave(buildArtwork(), artworkRepository);
    }

    @Test
    public void saveUser() throws Exception {
        doTestSave(buildUser(), userRepository);
    }

    private <T extends Identity<K>, K extends Serializable> void doTestSave(T entity, CrudRepository<T, K> repository) {
        repository.save(entity);
        assertThat(repository.findOne(entity.getId())).isNotNull();
    }

    private Installation buildInstallation() {
        return Installation.builder()
                .version("1.0")
                .encryptionKey("someKey")
                .build();
    }

    private Config buildConfig() {
        return Config.builder()
                .id("someConfig")
                .value("someValue")
                .build();
    }

    private LogMessage buildLogMessage() {
        return LogMessage.builder()
                .type(LogMessage.Type.DEBUG)
                .code("someCode")
                .text("someText")
                .build();
    }

    private User buildUser() {
        return User.builder()
                .name("someName")
                .email("someEmail")
                .password("somePassword")
                .addRoles(User.Role.USER, User.Role.ADMIN)
                .build();
    }

    private Artwork buildArtwork() {
        return Artwork.builder()
                .mimeType("text/plain")
                .checksum("123")
                .largeImageSize(123L)
                .largeImagePath("/largePath")
                .smallImageSize(12L)
                .smallImagePath("/smallPath")
                .tag("someTag")
                .putMetaData("k1", "v1")
                .putMetaData("k2", "v2")
                .build();
    }

    private Genre buildGenre() {
        return Genre.builder().build();
    }

    private Artist buildArtist() {
        return Artist.builder().build();
    }

    private Album buildAlbum(Artist artist) {
        return Album.builder().artist(artist).build();
    }

    private Song buildSong(Album album, Genre genre) {
        return Song.builder()
                .album(album)
                .genre(genre)
                .mimeType("text/plain")
                .duration(100L)
                .size(10L)
                .bitRate(256L)
                .bitRateVariable(false)
                .path("/dev/null")
                .build();
    }
}
