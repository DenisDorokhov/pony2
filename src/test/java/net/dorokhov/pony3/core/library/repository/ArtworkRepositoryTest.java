package net.dorokhov.pony3.core.library.repository;

import net.dorokhov.pony3.IntegrationTest;
import net.dorokhov.pony3.api.library.domain.Artwork;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ArtworkRepositoryTest extends IntegrationTest {

    @Autowired
    private ArtworkRepository artworkRepository;

    @Test
    public void shouldSave() {

        Artwork artwork = artworkRepository.save(new Artwork()
                .setMimeType("text/plain")
                .setChecksum("123")
                .setLargeImageSize(123L)
                .setLargeImagePath("/largePath")
                .setSmallImageSize(12L)
                .setSmallImagePath("/smallPath")
                .setSourceUri(UriComponentsBuilder
                        .fromUriString("file:sourceUri")
                        .build().toUri()));

        assertThat(artworkRepository.findById(artwork.getId())).isNotEmpty();
    }
}
