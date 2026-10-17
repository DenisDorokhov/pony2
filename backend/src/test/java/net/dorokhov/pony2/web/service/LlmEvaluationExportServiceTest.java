package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.core.library.repository.*;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static net.dorokhov.pony2.core.library.PlatformTransactionManagerFixtures.transactionManager;
import static org.mockito.Mockito.*;

class LlmEvaluationExportServiceTest {

    private final LocalDateTime exportedAt = LocalDateTime.of(2020, 1, 1, 0, 0);
    private final GenreRepository genreRepository = mock(GenreRepository.class);
    private final ArtistRepository artistRepository = mock(ArtistRepository.class);
    private final AlbumRepository albumRepository = mock(AlbumRepository.class);
    private final SongRepository songRepository = mock(SongRepository.class);
    private final DiscoveryTaskRepository taskRepository = mock(DiscoveryTaskRepository.class);
    private final ArtistGenreRepository artistGenreRepository = mock(ArtistGenreRepository.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final LlmEvaluationExportService service = new LlmEvaluationExportService(
            genreRepository, artistRepository, albumRepository, songRepository,
            taskRepository, artistGenreRepository, transactionManager(), jsonMapper);

    @Test
    void shouldStopReadingAfterClientDisconnects() {
        prepareArtist("x".repeat(65536));

        assertThatThrownBy(() -> service.write(disconnected(), exportedAt)).hasRootCauseInstanceOf(IOException.class);

        verify(taskRepository, never()).findArtistIdsForEvaluation(eq("artist"), any());
        verify(taskRepository, never()).findAlbumIdsForEvaluation(any(), any());
        verify(taskRepository, never()).findLatestCompletedByArtistIdAndType(
                eq("artist"), eq(DiscoveryTaskType.SPOTIFY_TOP_TRACKS), any());
    }

    @Test
    void shouldStopReadingAlbumTasksAfterClientDisconnects() {
        Artist artist = artist();
        Album album = new Album().setId("album").setCreationDate(exportedAt).setArtist(artist);
        when(taskRepository.findAlbumIdsForEvaluation(eq(""), any())).thenReturn(List.of("album"));
        when(albumRepository.findById("album")).thenReturn(Optional.of(album));
        when(taskRepository.findLatestCompletedByAlbumIdAndType(
                eq("album"), eq(DiscoveryTaskType.SPOTIFY_ARTIST_DATA), any()))
                .thenReturn(Optional.of(task("x".repeat(65536))));

        assertThatThrownBy(() -> service.write(disconnected(), exportedAt)).hasRootCauseInstanceOf(IOException.class);

        verify(taskRepository, never()).findAlbumIdsForEvaluation(eq("album"), any());
        verify(taskRepository, never()).findLatestCompletedByAlbumIdAndType(
                eq("album"), eq(DiscoveryTaskType.SPOTIFY_TOP_TRACKS), any());
    }

    @Test
    void shouldWriteFormattedJson() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        service.write(output, exportedAt);

        String json = output.toString(StandardCharsets.UTF_8);
        assertThat(json).contains(System.lineSeparator() + "  \"exportedAt\" : \"2020-01-01T00:00\"");
        assertThat(json).endsWith(System.lineSeparator() + "}");
        assertThat(jsonMapper.readTree(json).properties()).hasSize(5);
    }

    @Test
    void shouldFlushHeaderBeforeLoadingExportData() {
        AtomicBoolean headerFlushed = new AtomicBoolean();
        OutputStream output = new ByteArrayOutputStream() {
            @Override
            public void flush() throws IOException {
                headerFlushed.set(true);
                super.flush();
            }
        };
        when(taskRepository.findGenreIdsForEvaluation(eq(""), any())).thenAnswer(invocation -> {
            assertThat(headerFlushed).isTrue();
            return List.of();
        });

        service.write(output, exportedAt);
    }

    @Test
    void shouldLeaveDocumentIncompleteWhenNextPageFails() {
        prepareArtist("response");
        when(taskRepository.findArtistIdsForEvaluation(eq("artist"), any()))
                .thenThrow(new IllegalStateException("Database unavailable"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThatThrownBy(() -> service.write(output, exportedAt)).isInstanceOf(IllegalStateException.class);

        String partial = output.toString(StandardCharsets.UTF_8);
        assertThat(partial).contains("rawResult");
        assertThatThrownBy(() -> jsonMapper.readTree(partial)).isInstanceOf(JacksonException.class);
    }

    private void prepareArtist(String rawResult) {
        when(taskRepository.findArtistIdsForEvaluation(eq(""), any())).thenReturn(List.of("artist"));
        when(artistRepository.findById("artist")).thenReturn(Optional.of(artist()));
        when(taskRepository.findLatestCompletedByArtistIdAndType(
                eq("artist"), eq(DiscoveryTaskType.SPOTIFY_ARTIST_DATA), any()))
                .thenReturn(Optional.of(task(rawResult)));
    }

    private Artist artist() {
        return new Artist().setId("artist").setCreationDate(exportedAt).setName("Artist");
    }

    private DiscoveryTask task(String rawResult) {
        return new DiscoveryTask().setId("task").setCreationDate(exportedAt).setJob(new DiscoveryJob().setId("job"))
                .setType(DiscoveryTaskType.SPOTIFY_ARTIST_DATA).setStatus(DiscoveryTask.Status.COMPLETE)
                .setParameter("{}").setResult("{}").setRawRequest("[]").setRawResult(rawResult);
    }

    private OutputStream disconnected() {
        return new OutputStream() {
            private int remaining = 1024;

            @Override
            public void write(int value) throws IOException {
                if (--remaining < 0) {
                    throw new IOException("Connection reset by peer");
                }
            }
        };
    }
}
