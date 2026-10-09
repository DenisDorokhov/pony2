package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.api.library.service.DiscoveryJobService;
import net.dorokhov.pony2.api.library.service.ScanJobService;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentLibraryJobException;
import net.dorokhov.pony2.core.library.repository.DiscoveryJobRepository;
import net.dorokhov.pony2.core.library.repository.ScanJobRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

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
