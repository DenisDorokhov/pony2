package net.dorokhov.pony.core.library.repository;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.api.library.domain.*;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

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
    public void shouldSave() throws Exception {
        Genre genre = genreRepository.save(Genre.builder().build());
        Artist artist = artistRepository.save(Artist.builder().build());
        Album album = albumRepository.save(Album.builder()
                .artist(artist)
                .build());
        Song song = songRepository.save(Song.builder()
                .album(album)
                .genre(genre)
                .fileType(FileType.of("text/plain", "txt"))
                .duration(100L)
                .size(10L)
                .bitRate(256L)
                .bitRateVariable(false)
                .path("/dev/null")
                .build());
        assertThat(songRepository.findOne(song.getId())).isNotNull();
    }
}
