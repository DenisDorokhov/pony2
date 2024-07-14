package net.dorokhov.pony3.core.library.repository;

import net.dorokhov.pony3.IntegrationTest;
import net.dorokhov.pony3.api.library.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SongRepositoryTest extends IntegrationTest {
    
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private SongRepository songRepository;

    @Test
    public void shouldSave() {

        Genre genre = genreRepository.save(new Genre());
        Artist artist = artistRepository.save(new Artist());
        Album album = albumRepository.save(new Album()
                .setArtist(artist));

        Song song = songRepository.save(new Song()
                .setAlbum(album)
                .setGenre(genre)
                .setFileType(FileType.of("text/plain", "txt"))
                .setDuration(100L)
                .setSize(10L)
                .setBitRate(256L)
                .setBitRateVariable(false)
                .setPath("/dev/null"));

        assertThat(songRepository.findById(song.getId())).isNotEmpty();
    }
}
