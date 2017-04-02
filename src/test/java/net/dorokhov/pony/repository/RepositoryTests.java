package net.dorokhov.pony.repository;

import com.google.common.collect.ImmutableSet;
import net.dorokhov.pony.entity.*;
import net.dorokhov.pony.test.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryTests extends IntegrationTest {

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

    private <T extends Identifiable<K>, K extends Serializable> void doTestSave(T entity, CrudRepository<T, K> repository) {
        repository.save(entity);
        assertThat(repository.findOne(entity.getId())).isNotNull();
    }

    private Installation buildInstallation() {
        return new Installation("1.0");
    }

    private Config buildConfig() {
        return new Config("someConfig", "someValue");
    }

    private LogMessage buildLogMessage() {
        return new LogMessage(LogMessage.Type.DEBUG, "someCode");
    }

    private User buildUser() {
        
        User user = new User();
        
        user.setName("someName");
        user.setEmail("someEmail");
        user.setPassword("somePassword");
        user.setRoles(ImmutableSet.of(User.Role.USER, User.Role.ADMIN));
        
        return user;
    }

    private Artwork buildArtwork() {

        Artwork artwork = new Artwork();

        artwork.setName("foobar");
        artwork.setMimeType("text/plain");
        artwork.setChecksum("123");
        artwork.setLargeImageSize(123L);
        artwork.setLargeImagePath("/largePath");
        artwork.setSmallImageSize(12L);
        artwork.setSmallImagePath("/smallPath");
        artwork.setTag("someTag");
        artwork.getMetaData().put("k1", "v1");
        artwork.getMetaData().put("k2", "v2");

        return artwork;
    }

    private Genre buildGenre() {
        return new Genre();
    }

    private Artist buildArtist() {
        return new Artist();
    }

    private Album buildAlbum(Artist artist) {
        return new Album(artist);
    }

    private Song buildSong(Album album, Genre genre) {
        
        Song song = new Song(album, genre);
        
        song.setMimeType("text/plain");
        song.setDuration(100L);
        song.setSize(10L);
        song.setBitRate(256L);
        song.setBitRateVariable(false);
        song.setPath("/dev/null");
        
        return song;
    }
}
