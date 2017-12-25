package net.dorokhov.pony.core.library.repository;

import net.dorokhov.pony.api.library.domain.Artist;
import net.dorokhov.pony.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtistRepositoryTest extends IntegrationTest {
    
    @Autowired
    private ArtistRepository artistRepository;

    @Test
    public void shouldSave() throws Exception {
        Artist artist = artistRepository.save(Artist.builder().build());
        assertThat(artistRepository.findOne(artist.getId())).isNotNull();
    }
}
