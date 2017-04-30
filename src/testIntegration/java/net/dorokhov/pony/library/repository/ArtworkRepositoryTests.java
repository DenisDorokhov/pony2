package net.dorokhov.pony.library.repository;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.library.domain.Artwork;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtworkRepositoryTests extends IntegrationTest {

    @Autowired
    private ArtworkRepository artworkRepository;

    @Test
    public void save() throws Exception {
        Artwork artwork = artworkRepository.save(Artwork.builder()
                .mimeType("text/plain")
                .checksum("123")
                .largeImageSize(123L)
                .largeImagePath("/largePath")
                .smallImageSize(12L)
                .smallImagePath("/smallPath")
                .sourceUri("sourceUri")
                .build());
        assertThat(artworkRepository.findOne(artwork.getId())).isNotNull();
    }
}
