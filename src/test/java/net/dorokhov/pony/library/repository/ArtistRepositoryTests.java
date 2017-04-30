package net.dorokhov.pony.library.repository;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.library.domain.Artist;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtistRepositoryTests extends IntegrationTest {
    
    @Autowired
    private ArtistRepository artistRepository;

    @Test
    public void save() throws Exception {
        Artist artist = artistRepository.save(Artist.builder().build());
        assertThat(artistRepository.findOne(artist.getId())).isNotNull();
    }
}
