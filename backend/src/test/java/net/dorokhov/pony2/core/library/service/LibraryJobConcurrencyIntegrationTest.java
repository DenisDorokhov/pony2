package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.config.service.command.ConfigSetUpdateCommand;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.service.DiscoveryJobService;
import net.dorokhov.pony2.api.library.service.ScanJobService;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentLibraryJobException;
import net.dorokhov.pony2.core.library.repository.DiscoveryJobRepository;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.ScanJobRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class LibraryJobConcurrencyIntegrationTest extends InstallingIntegrationTest {

    private enum JobKind { SCAN, EDIT, DISCOVERY }

    @Autowired
    private ScanJobService scanService;
    @Autowired
    private DiscoveryJobService discoveryService;
    @Autowired
    private ScanJobRepository scanJobRepository;
    @Autowired
    private DiscoveryJobRepository discoveryJobRepository;
    @Autowired
    private LibraryJobSynchronizer jobSynchronizer;
    @Autowired
    private ConfigService configService;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;

    @Test
    void shouldInterruptDiscoveryWhenDisabledInConfig() throws Exception {
        configService.update(new ConfigSetUpdateCommand()
                .setDiscoveryEnabled(true)
                .setLlmUrl("http://localhost/v1")
                .setLlmModel("model"));
        Artist artist = artistRepository.save(new Artist().setName("Artist"));
        albumRepository.save(new Album().setArtist(artist).setName("Album"));
        CountDownLatch callStarted = new CountDownLatch(1);
        CountDownLatch callCancelled = new CountDownLatch(1);
        when(model.getOptions()).thenReturn(OpenAiChatOptions.builder().build());
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            try (LibraryJobSynchronizer.CancellationSubscription ignored = jobSynchronizer.onCancel(callCancelled::countDown)) {
                callStarted.countDown();
                assertThat(callCancelled.await(10, SECONDS)).isTrue();
                assertThat(configService.get().discoveryEnabled()).isTrue();
                jobSynchronizer.interruptDiscoveryIfCancelled();
                return null;
            }
        });
        DiscoveryJob job = discoveryService.startArtistJob(artist.getId(), false);
        try {
            assertThat(callStarted.await(10, SECONDS)).isTrue();

            configService.update(new ConfigSetUpdateCommand()
                    .setLlmUrl("http://localhost/v1")
                    .setLlmModel("model"));

            await().atMost(10, SECONDS).untilAsserted(() ->
                    assertThat(discoveryService.getById(job.getId()).orElseThrow().getStatus())
                            .isEqualTo(DiscoveryJob.Status.INTERRUPTED));
            assertThat(configService.get().discoveryEnabled()).isFalse();
        } finally {
            jobSynchronizer.cancelDiscovery();
            callCancelled.countDown();
            await().atMost(10, SECONDS).until(() -> discoveryService.getCurrentDiscoveryJobProgress().isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldStopDiscoveryBeforeSavingConfig(boolean rollback) throws ConcurrentLibraryJobException {
        configService.update(new ConfigSetUpdateCommand()
                .setDiscoveryEnabled(true)
                .setLlmUrl("http://localhost/v1")
                .setLlmModel("model"));
        AtomicBoolean cancelled = new AtomicBoolean();
        try (
                LibraryJobSynchronizer.LibraryJobRegistration registration = jobSynchronizer.registerDiscoveryJob();
                LibraryJobSynchronizer.CancellationSubscription subscription = jobSynchronizer.onCancel(() -> {
                    assertThat(configService.get().discoveryEnabled()).isTrue();
                    cancelled.set(true);
                    registration.close();
                })
        ) {
            getTransactionTemplate().executeWithoutResult(status -> {
                configService.update(new ConfigSetUpdateCommand()
                        .setLlmUrl("http://localhost/v1")
                        .setLlmModel("model"));
                assertThat(cancelled).isTrue();
                if (rollback) {
                    status.setRollbackOnly();
                }
            });

            assertThat(cancelled).isTrue();
            assertThat(jobSynchronizer.isDiscoveryCancelled()).isTrue();
            assertThat(configService.get().discoveryEnabled()).isEqualTo(rollback);
        }
    }

    @ParameterizedTest
    @EnumSource(JobKind.class)
    void shouldHoldSharedLockUntilOuterTransactionRollsBack(JobKind kind) throws Exception {
        getTransactionTemplate().executeWithoutResult(status -> {
            if (kind != JobKind.DISCOVERY) {
                if (kind == JobKind.SCAN) {
                    assertThatCode(() -> scanService.startScanJob()).doesNotThrowAnyException();
                } else {
                    assertThatCode(() -> scanService.startEditJob(List.of())).doesNotThrowAnyException();
                }
                assertThat(scanJobRepository.count()).isEqualTo(1);
                assertThatThrownBy(() -> discoveryService.startFullJob()).isInstanceOf(ConcurrentLibraryJobException.class);
                assertThatThrownBy(() -> discoveryService.startArtistJob("artist")).isInstanceOf(ConcurrentLibraryJobException.class);
                assertThatThrownBy(() -> discoveryService.startAlbumJob("album")).isInstanceOf(ConcurrentLibraryJobException.class);
                assertThat(discoveryJobRepository.count()).isZero();
            } else {
                assertThatCode(() -> discoveryService.startFullJob()).doesNotThrowAnyException();
                assertThat(discoveryJobRepository.count()).isEqualTo(1);
                assertThatThrownBy(() -> jobSynchronizer.registerScanJob(Duration.ZERO))
                        .isInstanceOf(ConcurrentLibraryJobException.class);
                assertThat(scanJobRepository.count()).isZero();
            }
            assertThatThrownBy(jobSynchronizer::registerDiscoveryJob).isInstanceOf(ConcurrentLibraryJobException.class);
            assertThat(scanService.getCurrentScanJobProgress()).isEmpty();
            assertThat(discoveryService.getCurrentDiscoveryJobProgress()).isEmpty();
            status.setRollbackOnly();
        });

        assertThat(scanJobRepository.count()).isZero();
        assertThat(discoveryJobRepository.count()).isZero();
        try (LibraryJobSynchronizer.LibraryJobRegistration ignored = jobSynchronizer.registerDiscoveryJob()) {
            assertThatThrownBy(jobSynchronizer::registerDiscoveryJob).isInstanceOf(ConcurrentLibraryJobException.class);
        }
    }
}
