package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
import net.dorokhov.pony2.api.library.domain.DiscoveryType;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.ARTIST_DISCOVERY;
import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.FULL_DISCOVERY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FullDiscoveryServiceTest {

    @InjectMocks
    private FullDiscoveryService fullDiscoveryService;

    @Mock
    private LibraryJobSynchronizer jobSynchronizer;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private ArtistDiscoveryService artistDiscoveryService;
    @Mock
    private LibraryJobSynchronizer.DiscoveryTaskRegistration taskRegistration;
    @Test
    public void shouldDelegateIncrementalArtistDiscovery() {
        DiscoveryJob job = discoveryJob();
        Artist artist = artist("artist1");
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));

        fullDiscoveryService.discover(job, false, null);

        verify(artistRepository).findAll(Sort.by("id"));
        verify(artistDiscoveryService).discoverIfUpdated(eq(job), eq(artist), eq(List.of()), eq(false),
                isNull());
        verify(jobSynchronizer, times(2)).interruptDiscoveryIfCancelled();
    }

    @Test
    public void shouldReportEmptyLibraryProgress() {
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of());
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(discoveryJob(), progressCalls::add);

        assertThat(progressCalls).extracting(DiscoveryProgress::getValue)
                .containsExactly(DiscoveryProgress.Value.of(0, 0));
        verifyNoInteractions(artistDiscoveryService);
    }

    @Test
    public void shouldAggregateTaskProgressAndCountSkippedParts() {
        DiscoveryJob job = discoveryJob();
        Artist first = artist("artist1");
        Artist second = artist("artist2");
        List<Album> firstAlbums = List.of(album("album1", first), album("album2", first));
        List<Album> secondAlbums = List.of(album("album3", second));
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(first, second));
        when(albumRepository.count()).thenReturn(3L);
        when(albumRepository.findByArtistId("artist1")).thenReturn(firstAlbums);
        when(albumRepository.findByArtistId("artist2")).thenReturn(secondAlbums);
        doAnswer(invocation -> {
            Consumer<DiscoveryProgress> observer = invocation.getArgument(4);
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 4)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(1, 4)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(4, 4)));
            return null;
        }).when(artistDiscoveryService).discoverIfUpdated(eq(job), eq(first), eq(firstAlbums), eq(true),
                any());
        doAnswer(invocation -> {
            Consumer<DiscoveryProgress> observer = invocation.getArgument(4);
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(2, 3)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(3, 3)));
            return null;
        }).when(artistDiscoveryService).discoverIfUpdated(eq(job), eq(second), eq(secondAlbums), eq(true),
                any());
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(job, progressCalls::add);

        assertThat(progressCalls).extracting(DiscoveryProgress::getStep).containsOnly(FULL_DISCOVERY);
        assertThat(progressCalls).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, 7), DiscoveryProgress.Value.of(0, 7),
                DiscoveryProgress.Value.of(1, 7), DiscoveryProgress.Value.of(4, 7),
                DiscoveryProgress.Value.of(4, 7), DiscoveryProgress.Value.of(6, 7),
                DiscoveryProgress.Value.of(7, 7), DiscoveryProgress.Value.of(7, 7));
    }

    @Test
    public void shouldStopBeforeArtistDiscoveryWhenCancelled() {
        when(jobSynchronizer.registerDiscoveryTask()).thenReturn(taskRegistration);
        doThrow(new DiscoveryInterruptedException()).when(jobSynchronizer).interruptDiscoveryIfCancelled();
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist("artist1")));
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        assertThatThrownBy(() -> fullDiscoveryService.discover(discoveryJob(), progressCalls::add))
                .isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(progressCalls).extracting(DiscoveryProgress::getValue)
                .containsExactly(DiscoveryProgress.Value.of(0, 2));
        verifyNoInteractions(artistDiscoveryService);
        verify(albumRepository, never()).findByArtistId(any());
        verify(taskRegistration).close();
    }

    @Test
    public void shouldStopWhenArtistDiscoveryIsInterrupted() {
        DiscoveryJob job = discoveryJob();
        Artist first = artist("artist1");
        Artist second = artist("artist2");
        when(jobSynchronizer.registerDiscoveryTask()).thenReturn(taskRegistration);
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(first, second));
        doAnswer(invocation -> {
            Consumer<DiscoveryProgress> observer = invocation.getArgument(4);
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(1, 2)));
            throw new DiscoveryInterruptedException();
        }).when(artistDiscoveryService).discoverIfUpdated(eq(job), eq(first), eq(List.of()), eq(true),
                any());
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        assertThatThrownBy(() -> fullDiscoveryService.discover(job, progressCalls::add))
                .isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(progressCalls).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, 4), DiscoveryProgress.Value.of(1, 4));
        verify(artistDiscoveryService, never()).discoverIfUpdated(eq(job), eq(second), anyList(), anyBoolean(), any());
        verify(taskRegistration).close();
    }

    @Test
    public void shouldHandleUnknownAndEmptyArtistProgress() {
        DiscoveryJob job = discoveryJob();
        Artist artist = artist("artist1");
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        doAnswer(invocation -> {
            Consumer<DiscoveryProgress> observer = invocation.getArgument(4);
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, null));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 0)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(1, 2)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(2, 2)));
            return null;
        }).when(artistDiscoveryService).discoverIfUpdated(eq(job), eq(artist), eq(List.of()), eq(true),
                any());
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(job, progressCalls::add);

        assertThat(progressCalls).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, 2), DiscoveryProgress.Value.of(1, 2),
                DiscoveryProgress.Value.of(2, 2), DiscoveryProgress.Value.of(2, 2));
    }

    @Test
    public void shouldCountArtistTasksWithoutAlbums() {
        DiscoveryJob job = discoveryJob();
        Artist artist = artist("artist1");
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(job, progressCalls::add);

        assertThat(progressCalls).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, 2), DiscoveryProgress.Value.of(2, 2));
        verify(artistDiscoveryService).discoverIfUpdated(eq(job), eq(artist), eq(List.of()), eq(true),
                any());
    }

    private DiscoveryJob discoveryJob() {
        return new DiscoveryJob().setType(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.STARTED);
    }

    private Artist artist(String id) {
        return new Artist().setId(id).setName("Artist").setCreationDate(LocalDateTime.now().minusDays(1));
    }

    private Album album(String id, Artist artist) {
        return new Album().setId(id).setArtist(artist).setName(id).setCreationDate(artist.getCreationDate());
    }
}
