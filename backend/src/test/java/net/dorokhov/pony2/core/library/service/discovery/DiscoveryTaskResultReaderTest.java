package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.api.library.domain.ArtistBiographyData;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.api.library.domain.ImageFace;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DiscoveryTaskResultReaderTest {

    @Mock
    private DiscoveryTaskRepository taskRepository;

    private DiscoveryTaskResultReader reader;

    @BeforeEach
    void setUp() {
        reader = new DiscoveryTaskResultReader(taskRepository);
    }

    @Test
    void shouldReadBiographyWithoutLoadingTaskHistory() {
        when(taskRepository.findLatestCompletedResultByArtistIdAndType("artist", DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .thenReturn(Optional.of(JsonConverter.toJson(Map.of(
                        "biography", "Biography\n\nSecond paragraph", "links", List.of(),
                        "topTracks", List.of(Map.of("id", "track"))))));

        ArtistBiographyData result = reader.readArtistBiography("artist", null);

        assertThat(result.text()).isEqualTo("Biography\n\nSecond paragraph");
        assertThat(result.faces()).isNull();
        verify(taskRepository, never()).findLatestCompletedByArtistIdAndType(anyString(), any(), any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    void shouldNormalizeMissingOrBlankTextToNull(String text) {
        when(taskRepository.findLatestCompletedResultByArtistIdAndType("artist", DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .thenReturn(Optional.of(JsonConverter.toJson(new Biography(text, null))));

        ArtistBiographyData result = reader.readArtistBiography("artist", null);

        assertThat(result.text()).isNull();
        assertThat(result.links()).isEmpty();
    }

    @Test
    void shouldReadLinksOnlyBiography() {
        SpotifyArtistData.ExternalLink instagram = new SpotifyArtistData.ExternalLink("Instagram", "https://www.instagram.com/artist");
        when(taskRepository.findLatestCompletedResultByArtistIdAndType("artist", DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .thenReturn(Optional.of(JsonConverter.toJson(new Biography(null, List.of(instagram)))));

        ArtistBiographyData result = reader.readArtistBiography("artist", null);

        assertThat(result.text()).isNull();
        assertThat(result.links()).containsExactly(new ArtistBiographyData.Link(ArtistBiographyData.LinkType.INSTAGRAM, instagram.url()));
    }

    @Test
    void shouldReturnEmptyBiographyWhenNoCompletedResultExists() {
        ArtistBiographyData result = reader.readArtistBiography("artist", "image");

        assertThat(result.externalImageId()).isEqualTo("image");
        assertThat(result.text()).isNull();
        assertThat(result.links()).isEmpty();
        assertThat(result.faces()).isNull();
        assertThat(result.similarArtistSpotifyIds()).isEmpty();
    }

    @Test
    void shouldReadDistinctSimilarArtistSpotifyIdsInOriginalOrder() {
        when(taskRepository.findLatestCompletedResultByArtistIdAndType("artist", DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .thenReturn(Optional.of("""
                        {"similarArtists":[{"id":"second"},null,{"id":""},{"id":" "},{"name":"Missing ID"},
                        {"id":"first"},{"id":"second"}]}
                        """));

        ArtistBiographyData result = reader.readArtistBiography("artist", null);

        assertThat(result.similarArtistSpotifyIds()).containsExactly("second", "first");
        assertThat(result.text()).isNull();
        assertThat(result.links()).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"null", "{}", "{\"similarArtists\":null}", "{\"similarArtists\":[]}",
            "{\"similarArtists\":[null,{\"id\":\"\"},{\"id\":\" \"},{\"name\":\"Missing ID\"}]}"})
    void shouldReadEmptySimilarArtistsWhenResultOrFieldIsMissing(String result) {
        when(taskRepository.findLatestCompletedResultByArtistIdAndType("artist", DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .thenReturn(Optional.ofNullable(result));

        ArtistBiographyData biography = reader.readArtistBiography("artist", null);

        assertThat(biography.similarArtistSpotifyIds()).isEmpty();
    }

    @Test
    void shouldReturnFacesOnlyForCurrentImage() {
        List<ImageFace> faces = List.of(new ImageFace(.1, .2, .3, .4));
        when(taskRepository.findLatestCompletedResultByArtistIdAndType("artist", DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .thenReturn(Optional.empty());
        when(taskRepository.findLatestCompletedResultByArtistIdAndType(eq("artist"),
                eq(DiscoveryTaskType.ARTIST_BIO_IMAGE_FACES), any()))
                .thenReturn(Optional.of(JsonConverter.toJson(
                        new ArtistBioImageFacesService.Result("current", "key", faces))));

        assertThat(reader.readArtistBiography("artist", "current").faces()).containsExactlyElementsOf(faces);
        assertThat(reader.readArtistBiography("artist", "different").faces()).isNull();
    }

    @Test
    void shouldPreserveSuccessfulEmptyFaces() {
        when(taskRepository.findLatestCompletedResultByArtistIdAndType("artist", DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .thenReturn(Optional.empty());
        when(taskRepository.findLatestCompletedResultByArtistIdAndType("artist", DiscoveryTaskType.ARTIST_BIO_IMAGE_FACES, Limit.of(1)))
                .thenReturn(Optional.of(JsonConverter.toJson(
                        new ArtistBioImageFacesService.Result("image", "key", List.of()))));

        assertThat(reader.readArtistBiography("artist", "image").faces()).isEmpty();
    }

    private record Biography(String biography, List<SpotifyArtistData.ExternalLink> links) {}

    @Test
    void shouldReadBiographiesInOneBatchUsingTheSameContentRules() {
        DiscoveryTaskRepository.ArtistResult text = mock(DiscoveryTaskRepository.ArtistResult.class);
        when(text.getArtistId()).thenReturn("text");
        when(text.getResult()).thenReturn(JsonConverter.toJson(new Biography("Biography", null)));
        DiscoveryTaskRepository.ArtistResult links = mock(DiscoveryTaskRepository.ArtistResult.class);
        when(links.getArtistId()).thenReturn("links");
        when(links.getResult()).thenReturn(JsonConverter.toJson(new Biography(null,
                List.of(new SpotifyArtistData.ExternalLink("Instagram", "https://instagram.com/artist")))));
        DiscoveryTaskRepository.ArtistResult similarArtists = mock(DiscoveryTaskRepository.ArtistResult.class);
        when(similarArtists.getArtistId()).thenReturn("similarArtists");
        when(similarArtists.getResult()).thenReturn("{\"similarArtists\":[{\"id\":\"similar\"}]}");
        DiscoveryTaskRepository.ArtistResult empty = mock(DiscoveryTaskRepository.ArtistResult.class);
        when(empty.getArtistId()).thenReturn("empty");
        List<String> artistIds = List.of("text", "links", "similarArtists", "empty", "missing");
        when(taskRepository.findLatestCompletedResultsByArtistIdsAndType(artistIds, DiscoveryTaskType.SPOTIFY_ARTIST_DATA))
                .thenReturn(List.of(text, links, similarArtists, empty));

        assertThat(reader.readArtistBiographies(artistIds))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "text", new ArtistBiographyData(null, null, "Biography", List.of(), List.of()),
                        "links", new ArtistBiographyData(null, null, null,
                                List.of(new ArtistBiographyData.Link(ArtistBiographyData.LinkType.INSTAGRAM, "https://instagram.com/artist")),
                                List.of()),
                        "similarArtists", new ArtistBiographyData(null, null, null, List.of(), List.of("similar")),
                        "empty", new ArtistBiographyData(null, null, null, List.of(), List.of())));
        verify(taskRepository, never()).findLatestCompletedResultByArtistIdAndType(anyString(), any(), any());
    }

    @Test
    void shouldNotQueryBiographiesForEmptyArtistList() {
        assertThat(reader.readArtistBiographies(List.of())).isEmpty();
        verifyNoInteractions(taskRepository);
    }

    @ParameterizedTest
    @CsvSource({
            "https://www.instagram.com/artist,INSTAGRAM",
            "https://facebook.com/artist,FACEBOOK",
            "https://x.com/artist,X",
            "https://twitter.com/artist,X",
            "https://www.tiktok.com/@artist,TIKTOK",
            "https://en.wikipedia.org/wiki/Artist,WIKIPEDIA",
            "https://whatsapp.com/channel/artist,WHATSAPP",
            "https://example.com,WEBSITE",
            "https://fakeinstagram.com/artist,WEBSITE"
    })
    void shouldDetermineLinkTypeFromItsUrl(String url, ArtistBiographyData.LinkType type) {
        assertThat(ArtistBiographyData.LinkType.fromUrl(url)).isEqualTo(type);
    }
}
