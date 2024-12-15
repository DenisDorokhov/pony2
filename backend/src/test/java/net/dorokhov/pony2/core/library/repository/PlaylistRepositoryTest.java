package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

public class PlaylistRepositoryTest extends IntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private SongRepository songRepository;
    @Autowired
    private PlaylistRepository playlistRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    @Test
    public void shouldSave() {

        User user = userRepository.save(new User()
                .setName("user")
                .setEmail("email@email.com")
                .setPassword("password")
        );

        Genre genre = genreRepository.save(new Genre());
        Artist artist = artistRepository.save(new Artist());
        Album album = albumRepository.save(new Album().setArtist(artist));

        Song song1 = songRepository.save(new Song()
                .setAlbum(album)
                .setGenre(genre)
                .setBitRate(128L)
                .setBitRateVariable(false)
                .setPath("the foobar entity1")
                .setFileType(FileType.of("text/plain", "txt"))
                .setDuration(666L)
                .setSize(256L)
                .setName("the foobar entity1")
                .setArtistName("artist")
                .setAlbumArtistName("other")
                .setAlbumName("album")
        );
        Song song2 = songRepository.save(new Song()
                .setAlbum(album)
                .setGenre(genre)
                .setBitRate(128L)
                .setBitRateVariable(false)
                .setPath("the foobar entity2")
                .setFileType(FileType.of("text/plain", "txt"))
                .setDuration(666L)
                .setSize(256L)
                .setName("the foobar entity2")
                .setArtistName("artist")
                .setAlbumArtistName("other")
                .setAlbumName("album")
        );

        Playlist playlist = new Playlist()
                .setName("playlist")
                .setType(Playlist.Type.NORMAL)
                .setUser(user);
        playlist.setSongs(List.of(
                new PlaylistSong()
                        .setSort(0)
                        .setSong(song1)
                        .setPlaylist(playlist),
                new PlaylistSong()
                        .setSort(1)
                        .setSong(song2)
                        .setPlaylist(playlist),
                new PlaylistSong()
                        .setSort(2)
                        .setSong(song1)
                        .setPlaylist(playlist)
        ));
        playlistRepository.save(playlist);

        transactionTemplate.executeWithoutResult(status ->
                assertThat(playlistRepository.findById(playlist.getId())).hasValueSatisfying(foundPlaylist -> {
                    assertThat(foundPlaylist.getSongs()).hasSize(3);
                    assertThat(foundPlaylist.getSongs().get(0).getSong().getName()).isEqualTo("the foobar entity1");
                    assertThat(foundPlaylist.getSongs().get(1).getSong().getName()).isEqualTo("the foobar entity2");
                    assertThat(foundPlaylist.getSongs().get(2).getSong().getName()).isEqualTo("the foobar entity1");
                }));

        playlistRepository.save(playlist
                .setSongs(List.of(
                        new PlaylistSong()
                                .setSort(1)
                                .setSong(song2)
                                .setPlaylist(playlist),
                        new PlaylistSong()
                                .setSort(2)
                                .setSong(song1)
                                .setPlaylist(playlist)
                ))
        );

        transactionTemplate.executeWithoutResult(status ->
                assertThat(playlistRepository.findById(playlist.getId())).hasValueSatisfying(foundPlaylist -> {
                    assertThat(foundPlaylist.getSongs()).hasSize(2);
                    assertThat(foundPlaylist.getSongs().get(0).getSong().getName()).isEqualTo("the foobar entity2");
                    assertThat(foundPlaylist.getSongs().get(1).getSong().getName()).isEqualTo("the foobar entity1");
                }));
    }
}
