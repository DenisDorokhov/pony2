package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtistDiscovery;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
import net.dorokhov.pony2.api.library.domain.DiscoveryType;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.repository.ArtistDiscoveryRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.ARTIST_DISCOVERY;
import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.FULL_ARTIST_DISCOVERY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FullDiscoveryServiceTest {

    @Mock
    private LibraryJobSynchronizer jobSynchronizer;

    @InjectMocks
    private FullDiscoveryService fullDiscoveryService;

    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private ArtistDiscoveryRepository artistDiscoveryRepository;
    @Mock
    private ArtistDiscoveryService artistDiscoveryService;
    @Mock
    private LibraryJobSynchronizer.DiscoveryTaskRegistration taskRegistration;

    @Test
    public void shouldDiscoverArtists() {

        DiscoveryJob discoveryJob = discoveryJob();
        Artist artist1 = artist("artist1");
        Artist artist2 = artist("artist2");

        when(jobSynchronizer.registerDiscoveryTask()).thenReturn(taskRegistration);
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist1, artist2));
        when(artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc(any())).thenReturn(Optional.empty());

        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(discoveryJob, progressCalls::add);

        verify(artistRepository).findAll(Sort.by("id"));
        verify(artistDiscoveryRepository).findFirstByArtistIdOrderByCreationDateDesc("artist1");
        verify(artistDiscoveryRepository).findFirstByArtistIdOrderByCreationDateDesc("artist2");
        InOrder inOrder = inOrder(jobSynchronizer, artistDiscoveryService, taskRegistration);
        inOrder.verify(jobSynchronizer).registerDiscoveryTask();
        inOrder.verify(jobSynchronizer).interruptDiscoveryIfCancelled();
        inOrder.verify(artistDiscoveryService).discover(eq(discoveryJob), eq(artist1), eq(true), any());
        inOrder.verify(jobSynchronizer).interruptDiscoveryIfCancelled();
        inOrder.verify(artistDiscoveryService).discover(eq(discoveryJob), eq(artist2), eq(true), any());
        inOrder.verify(taskRegistration).close();
        verifyNoMoreInteractions(artistDiscoveryService);

        assertThat(progressCalls).hasSize(3);
        assertProgress(progressCalls.get(0), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 200));
        assertProgress(progressCalls.get(1), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(100, 200));
        assertProgress(progressCalls.get(2), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(200, 200));
    }

    @Test
    public void shouldReportEmptyLibraryProgress() {

        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of());

        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(discoveryJob(), progressCalls::add);

        assertThat(progressCalls).hasSize(1);
        assertProgress(progressCalls.get(0), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 0));
        verifyNoInteractions(artistDiscoveryRepository, artistDiscoveryService);
    }

    @Test
    public void shouldStopBeforeArtistDiscoveryWhenCancelled() {

        when(jobSynchronizer.registerDiscoveryTask()).thenReturn(taskRegistration);
        doThrow(new DiscoveryInterruptedException()).when(jobSynchronizer).interruptDiscoveryIfCancelled();
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist("artist1")));
        when(artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc("artist1"))
                .thenReturn(Optional.empty());

        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        assertThatThrownBy(() -> fullDiscoveryService.discover(discoveryJob(), progressCalls::add))
                .isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(progressCalls).hasSize(1);
        assertProgress(progressCalls.get(0), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 100));
        verifyNoInteractions(artistDiscoveryService);
        verify(taskRegistration).close();
    }

    @Test
    public void shouldStopWhenArtistDiscoveryIsInterrupted() {

        DiscoveryJob discoveryJob = discoveryJob();
        Artist artist1 = artist("artist1");
        Artist artist2 = artist("artist2");
        when(jobSynchronizer.registerDiscoveryTask()).thenReturn(taskRegistration);
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist1, artist2));
        when(artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc("artist1"))
                .thenReturn(Optional.empty());
        doAnswer(invocation -> {
            Consumer<DiscoveryProgress> observer = invocation.getArgument(3);
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(1, 2)));
            throw new DiscoveryInterruptedException();
        }).when(artistDiscoveryService).discover(eq(discoveryJob), eq(artist1), eq(true), any());

        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        assertThatThrownBy(() -> fullDiscoveryService.discover(discoveryJob, progressCalls::add))
                .isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(progressCalls).hasSize(2);
        assertProgress(progressCalls.get(0), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 200));
        assertProgress(progressCalls.get(1), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(50, 200));
        verify(artistDiscoveryService).discover(eq(discoveryJob), eq(artist1), eq(true), any());
        verifyNoMoreInteractions(artistDiscoveryService);
        verify(artistDiscoveryRepository, never()).findFirstByArtistIdOrderByCreationDateDesc("artist2");
        verify(taskRegistration).close();
    }

    @Test
    public void shouldSkipUpToDateDiscoveries() {

        LocalDateTime now = LocalDateTime.now();
        Artist artist = artist("artist1").setUpdateDate(now.minusMinutes(1));

        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        when(artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc("artist1"))
                .thenReturn(Optional.of(new ArtistDiscovery().setCreationDate(now)));

        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        Thread.currentThread().interrupt();
        try {
            fullDiscoveryService.discover(discoveryJob(), progressCalls::add);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }

        assertThat(progressCalls).hasSize(2);
        assertProgress(progressCalls.get(0), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 100));
        assertProgress(progressCalls.get(1), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(100, 100));
        verifyNoInteractions(artistDiscoveryService);
        verify(jobSynchronizer, never()).interruptDiscoveryIfCancelled();
    }

    @Test
    public void shouldDiscoverOutdatedDiscoveries() {

        LocalDateTime now = LocalDateTime.now();
        DiscoveryJob discoveryJob = discoveryJob();
        Artist artist = artist("artist1").setUpdateDate(now);

        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        when(artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc("artist1"))
                .thenReturn(Optional.of(new ArtistDiscovery().setCreationDate(now.minusMinutes(1))));

        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(discoveryJob, progressCalls::add);

        verify(artistDiscoveryService).discover(eq(discoveryJob), eq(artist), eq(true), any());
        assertThat(progressCalls).hasSize(2);
        assertProgress(progressCalls.get(0), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 100));
        assertProgress(progressCalls.get(1), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(100, 100));
    }

    @Test
    public void shouldPassDisabledCacheAndSupportNullObserver() {

        DiscoveryJob discoveryJob = discoveryJob();
        Artist artist = artist("artist1");
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        when(artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc("artist1"))
                .thenReturn(Optional.empty());

        fullDiscoveryService.discover(discoveryJob, false, null);

        verify(artistDiscoveryService).discover(discoveryJob, artist, false, null);
    }

    @Test
    public void shouldPreserveDiscoveryDateBoundaryChecks() {

        LocalDateTime now = LocalDateTime.now();
        DiscoveryJob discoveryJob = discoveryJob();
        Artist unchangedArtist = artist("artist1").setUpdateDate(now);
        Artist artistWithoutUpdateDate = artist("artist2");
        Artist artistWithoutDiscoveryDate = artist("artist3").setUpdateDate(now);
        when(artistRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(unchangedArtist, artistWithoutUpdateDate, artistWithoutDiscoveryDate));
        when(artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc("artist1"))
                .thenReturn(Optional.of(new ArtistDiscovery().setCreationDate(now)));
        when(artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc("artist2"))
                .thenReturn(Optional.of(new ArtistDiscovery()));
        when(artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc("artist3"))
                .thenReturn(Optional.of(new ArtistDiscovery()));

        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(discoveryJob, progressCalls::add);

        verify(artistDiscoveryService).discover(eq(discoveryJob), eq(artistWithoutDiscoveryDate), eq(true), any());
        verifyNoMoreInteractions(artistDiscoveryService);
        assertThat(progressCalls).hasSize(4);
        assertProgress(progressCalls.get(3), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(300, 300));
    }

    @Test
    public void shouldAggregateArtistProgressAndCountSkippedArtists() {

        LocalDateTime now = LocalDateTime.now();
        DiscoveryJob discoveryJob = discoveryJob();
        Artist artist1 = artist("artist1");
        Artist skippedArtist = artist("artist2").setUpdateDate(now.minusMinutes(1));
        Artist artist3 = artist("artist3");
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist1, skippedArtist, artist3));
        when(artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc("artist1"))
                .thenReturn(Optional.empty());
        when(artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc("artist2"))
                .thenReturn(Optional.of(new ArtistDiscovery().setCreationDate(now)));
        when(artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc("artist3"))
                .thenReturn(Optional.empty());
        doAnswer(invocation -> {
            Consumer<DiscoveryProgress> observer = invocation.getArgument(3);
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 4)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(1, 4)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(4, 4)));
            return null;
        }).when(artistDiscoveryService).discover(eq(discoveryJob), eq(artist1), eq(true), any());
        doAnswer(invocation -> {
            Consumer<DiscoveryProgress> observer = invocation.getArgument(3);
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 2)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(1, 2)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(2, 2)));
            return null;
        }).when(artistDiscoveryService).discover(eq(discoveryJob), eq(artist3), eq(true), any());

        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(discoveryJob, progressCalls::add);

        assertThat(progressCalls).extracting(DiscoveryProgress::getStep).containsOnly(FULL_ARTIST_DISCOVERY);
        assertThat(progressCalls).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, 300),
                DiscoveryProgress.Value.of(0, 300),
                DiscoveryProgress.Value.of(25, 300),
                DiscoveryProgress.Value.of(100, 300),
                DiscoveryProgress.Value.of(100, 300),
                DiscoveryProgress.Value.of(200, 300),
                DiscoveryProgress.Value.of(200, 300),
                DiscoveryProgress.Value.of(250, 300),
                DiscoveryProgress.Value.of(300, 300),
                DiscoveryProgress.Value.of(300, 300));
        verify(artistDiscoveryService, never()).discover(eq(discoveryJob), eq(skippedArtist), anyBoolean(), any());
    }

    @Test
    public void shouldHandleUnknownAndEmptyArtistProgress() {

        DiscoveryJob discoveryJob = discoveryJob();
        Artist artist = artist("artist1");
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        doAnswer(invocation -> {
            Consumer<DiscoveryProgress> observer = invocation.getArgument(3);
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, null));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 0)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(1, 3)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(2, 3)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(3, 3)));
            return null;
        }).when(artistDiscoveryService).discover(eq(discoveryJob), eq(artist), eq(true), any());

        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(discoveryJob, progressCalls::add);

        assertThat(progressCalls).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, 100),
                DiscoveryProgress.Value.of(33, 100),
                DiscoveryProgress.Value.of(66, 100),
                DiscoveryProgress.Value.of(100, 100),
                DiscoveryProgress.Value.of(100, 100));
    }

    private void assertProgress(DiscoveryProgress progress, DiscoveryProgress.Step step, DiscoveryProgress.Value value) {
        assertThat(progress.getStep()).isSameAs(step);
        assertThat(progress.getValue()).isEqualTo(value);
    }

    private DiscoveryJob discoveryJob() {
        return new DiscoveryJob()
                .setType(DiscoveryType.FULL)
                .setStatus(DiscoveryJob.Status.STARTED);
    }

    private Artist artist(String id) {
        return new Artist()
                .setId(id)
                .setName("someArtist");
    }
}
