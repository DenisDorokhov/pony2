package net.dorokhov.pony3.core.library.repository;

import net.dorokhov.pony3.IntegrationTest;
import net.dorokhov.pony3.api.library.domain.Album;
import net.dorokhov.pony3.api.library.domain.Artist;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AlbumRepositoryTest extends IntegrationTest {
    
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;

    @Test
    public void shouldSave() {

        Artist artist = artistRepository.save(new Artist());
        Album album = albumRepository.save(new Album()
                .setArtist(artist));

        assertThat(albumRepository.findById(album.getId())).isNotEmpty();
    }
}
