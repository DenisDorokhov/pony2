package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.AlbumDiscovery;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtistDiscovery;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.core.library.repository.AlbumDiscoveryRepository;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistDiscoveryRepository;
import net.dorokhov.pony2.core.library.repository.ArtistGenreRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Limit;
import org.springframework.transaction.PlatformTransactionManager;
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

    private final LocalDateTime maximumCreationDate = LocalDateTime.of(2020, 1, 1, 0, 0);
    private final ArtistRepository artistRepository = mock(ArtistRepository.class);
    private final AlbumRepository albumRepository = mock(AlbumRepository.class);
    private final ArtistDiscoveryRepository artistDiscoveryRepository = mock(ArtistDiscoveryRepository.class);
    private final AlbumDiscoveryRepository albumDiscoveryRepository = mock(AlbumDiscoveryRepository.class);
    private final DiscoveryTaskRepository discoveryTaskRepository = mock(DiscoveryTaskRepository.class);
    private final ArtistGenreRepository artistGenreRepository = mock(ArtistGenreRepository.class);
    private final PlatformTransactionManager transactionManager = transactionManager();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final LlmEvaluationExportService service = new LlmEvaluationExportService(
            artistRepository,
            albumRepository,
            artistDiscoveryRepository,
            albumDiscoveryRepository,
            discoveryTaskRepository,
            artistGenreRepository,
            transactionManager,
            jsonMapper
    );

    @Test
    void shouldStopReadingAfterClientDisconnects() {
        prepareDiscovery();
        prepareTasks(task("x".repeat(65536)));
        OutputStream disconnected = new OutputStream() {
            private int remaining = 1024;

            @Override
            public void write(int value) throws IOException {
                if (--remaining < 0) {
                    throw new IOException("Connection reset by peer");
                }
            }
        };

        assertThatThrownBy(() -> service.write(disconnected, maximumCreationDate)).hasRootCauseInstanceOf(IOException.class);

        verify(artistDiscoveryRepository, never()).findTaskIdsForEvaluation(eq("discovery"), eq("task"), any(), any());
        verify(artistRepository, never()).findForEvaluation(eq("artist"), any());
        verify(albumRepository, never()).findForEvaluation(any(), any());
    }

    @Test
    void shouldStopReadingAlbumTasksAfterClientDisconnects() {
        Artist artist = new Artist().setId("artist").setCreationDate(maximumCreationDate).setName("Artist");
        Album album = new Album().setId("album").setCreationDate(maximumCreationDate).setName("Album").setArtist(artist);
        DiscoveryJob job = new DiscoveryJob().setId("job");
        AlbumDiscovery discovery = new AlbumDiscovery().setId("discovery").setCreationDate(maximumCreationDate)
                .setAlbum(album).setJob(job);
        when(albumRepository.findForEvaluation(eq(""), any())).thenReturn(List.of(album));
        when(albumDiscoveryRepository.findLatestForEvaluation("album", maximumCreationDate, Limit.of(1)))
                .thenReturn(Optional.of(discovery));
        when(albumDiscoveryRepository.findTaskIdsForEvaluation(
                eq("discovery"), eq(""), eq(maximumCreationDate), any())).thenReturn(List.of("task"));
        when(discoveryTaskRepository.findForEvaluationByIds(List.of("task")))
                .thenReturn(List.of(task("x".repeat(65536))));
        OutputStream disconnected = new OutputStream() {
            private int remaining = 1024;

            @Override
            public void write(int value) throws IOException {
                if (--remaining < 0) {
                    throw new IOException("Connection reset by peer");
                }
            }
        };

        assertThatThrownBy(() -> service.write(disconnected, maximumCreationDate)).hasRootCauseInstanceOf(IOException.class);

        verify(albumDiscoveryRepository, never()).findTaskIdsForEvaluation(eq("discovery"), eq("task"), any(), any());
        verify(albumRepository, never()).findForEvaluation(eq("album"), any());
    }

    @Test
    void shouldWriteFormattedJson() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        service.write(output, maximumCreationDate);

        String json = output.toString(StandardCharsets.UTF_8);
        assertThat(json).contains(System.lineSeparator() + "  \"exportedAt\" : \"2020-01-01T00:00\"");
        assertThat(json).endsWith(System.lineSeparator() + "}");
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
        when(artistRepository.findForEvaluation(eq(""), any())).thenAnswer(invocation -> {
            assertThat(headerFlushed).isTrue();
            return List.of();
        });

        service.write(output, maximumCreationDate);
    }

    @Test
    void shouldLeaveDocumentIncompleteWhenNextPageFails() {
        prepareDiscovery();
        prepareTasks(task("response"));
        when(artistDiscoveryRepository.findTaskIdsForEvaluation(
                eq("discovery"), eq("task"), eq(maximumCreationDate), any()))
                .thenThrow(new IllegalStateException("Database unavailable"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThatThrownBy(() -> service.write(output, maximumCreationDate)).isInstanceOf(IllegalStateException.class);

        String partial = output.toString(StandardCharsets.UTF_8);
        assertThat(partial).contains("rawResult").doesNotContain("\"complete\"");
        assertThatThrownBy(() -> jsonMapper.readTree(partial)).isInstanceOf(JacksonException.class);
    }

    private void prepareDiscovery() {
        Artist artist = new Artist().setId("artist").setCreationDate(maximumCreationDate).setName("Artist");
        DiscoveryJob job = new DiscoveryJob().setId("job");
        ArtistDiscovery discovery = new ArtistDiscovery().setId("discovery").setCreationDate(maximumCreationDate)
                .setArtist(artist).setJob(job);
        when(artistRepository.findForEvaluation(eq(""), any())).thenReturn(List.of(artist));
        when(artistDiscoveryRepository.findLatestForEvaluation("artist", maximumCreationDate, Limit.of(1)))
                .thenReturn(Optional.of(discovery));
    }

    private void prepareTasks(DiscoveryTask task) {
        when(artistDiscoveryRepository.findTaskIdsForEvaluation(
                eq("discovery"), eq(""), eq(maximumCreationDate), any()))
                .thenReturn(List.of("task"));
        when(discoveryTaskRepository.findForEvaluationByIds(List.of("task"))).thenReturn(List.of(task));
    }

    private DiscoveryTask task(String rawResult) {
        DiscoveryJob job = new DiscoveryJob().setId("job");
        return new DiscoveryTask().setId("task").setCreationDate(maximumCreationDate).setJob(job)
                .setType(DiscoveryTaskType.SPOTIFY_ARTIST_DATA).setStatus(DiscoveryTask.Status.COMPLETE)
                .setParameter("{}").setResult("{}").setRawRequest("[]").setRawResult(rawResult);
    }
}
