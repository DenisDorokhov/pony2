package net.dorokhov.pony.library.repository;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.library.domain.Album;
import net.dorokhov.pony.library.domain.Artist;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class AlbumRepositoryTests extends IntegrationTest {
    
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;

    @Test
    public void save() throws Exception {
        Artist artist = artistRepository.save(Artist.builder().build());
        Album album = albumRepository.save(Album.builder()
                .artist(artist)
                .build());
        assertThat(albumRepository.findOne(album.getId())).isNotNull();
    }
}
