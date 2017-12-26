package net.dorokhov.pony.core.library.repository;

import net.dorokhov.pony.api.library.domain.Artwork;
import net.dorokhov.pony.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtworkRepositoryTest extends IntegrationTest {

    @Autowired
    private ArtworkRepository artworkRepository;

    @Test
    public void shouldSave() {

        Artwork artwork = artworkRepository.save(Artwork.builder()
                .mimeType("text/plain")
                .checksum("123")
                .largeImageSize(123L)
                .largeImagePath("/largePath")
                .smallImageSize(12L)
                .smallImagePath("/smallPath")
                .sourceUri(UriComponentsBuilder
                        .fromUriString("file:sourceUri")
                        .build().toUri())
                .build());

        assertThat(artworkRepository.findOne(artwork.getId())).isNotNull();
    }
}
