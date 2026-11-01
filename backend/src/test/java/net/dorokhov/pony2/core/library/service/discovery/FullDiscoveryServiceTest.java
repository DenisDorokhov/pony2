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
import static net.dorokhov.pony2.core.library.service.discovery.AlbumDiscoveryService.ALBUM_TASK_COUNT;
import static net.dorokhov.pony2.core.library.service.discovery.ArtistDiscoveryService.ARTIST_TASK_COUNT;
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
        long firstTasksTotal = ARTIST_TASK_COUNT + (long) firstAlbums.size() * ALBUM_TASK_COUNT;
        long secondTasksTotal = ARTIST_TASK_COUNT + (long) secondAlbums.size() * ALBUM_TASK_COUNT;
        long tasksTotal = firstTasksTotal + secondTasksTotal;
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(first, second));
        when(albumRepository.count()).thenReturn(3L);
        when(albumRepository.findByArtistId("artist1")).thenReturn(firstAlbums);
        when(albumRepository.findByArtistId("artist2")).thenReturn(secondAlbums);
        doAnswer(invocation -> {
            Consumer<DiscoveryProgress> observer = invocation.getArgument(4);
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, firstTasksTotal)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(1, firstTasksTotal)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(firstTasksTotal, firstTasksTotal)));
            return null;
        }).when(artistDiscoveryService).discoverIfUpdated(eq(job), eq(first), eq(firstAlbums), eq(true),
                any());
        doAnswer(invocation -> {
            Consumer<DiscoveryProgress> observer = invocation.getArgument(4);
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(ARTIST_TASK_COUNT, secondTasksTotal)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(secondTasksTotal, secondTasksTotal)));
            return null;
        }).when(artistDiscoveryService).discoverIfUpdated(eq(job), eq(second), eq(secondAlbums), eq(true),
                any());
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(job, progressCalls::add);

        assertThat(progressCalls).extracting(DiscoveryProgress::getStep).containsOnly(FULL_DISCOVERY);
        assertThat(progressCalls).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, tasksTotal), DiscoveryProgress.Value.of(0, tasksTotal),
                DiscoveryProgress.Value.of(1, tasksTotal), DiscoveryProgress.Value.of(firstTasksTotal, tasksTotal),
                DiscoveryProgress.Value.of(firstTasksTotal, tasksTotal), DiscoveryProgress.Value.of(firstTasksTotal + ARTIST_TASK_COUNT, tasksTotal),
                DiscoveryProgress.Value.of(tasksTotal, tasksTotal), DiscoveryProgress.Value.of(tasksTotal, tasksTotal));
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
                .containsExactly(DiscoveryProgress.Value.of(0, ARTIST_TASK_COUNT));
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
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(1, ARTIST_TASK_COUNT)));
            throw new DiscoveryInterruptedException();
        }).when(artistDiscoveryService).discoverIfUpdated(eq(job), eq(first), eq(List.of()), eq(true),
                any());
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        assertThatThrownBy(() -> fullDiscoveryService.discover(job, progressCalls::add))
                .isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(progressCalls).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, 2L * ARTIST_TASK_COUNT), DiscoveryProgress.Value.of(1, 2L * ARTIST_TASK_COUNT));
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
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(1, ARTIST_TASK_COUNT)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(ARTIST_TASK_COUNT, ARTIST_TASK_COUNT)));
            return null;
        }).when(artistDiscoveryService).discoverIfUpdated(eq(job), eq(artist), eq(List.of()), eq(true),
                any());
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(job, progressCalls::add);

        assertThat(progressCalls).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, ARTIST_TASK_COUNT), DiscoveryProgress.Value.of(1, ARTIST_TASK_COUNT),
                DiscoveryProgress.Value.of(ARTIST_TASK_COUNT, ARTIST_TASK_COUNT), DiscoveryProgress.Value.of(ARTIST_TASK_COUNT, ARTIST_TASK_COUNT));
    }

    @Test
    public void shouldCountArtistTasksWithoutAlbums() {
        DiscoveryJob job = discoveryJob();
        Artist artist = artist("artist1");
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(job, progressCalls::add);

        assertThat(progressCalls).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, ARTIST_TASK_COUNT), DiscoveryProgress.Value.of(ARTIST_TASK_COUNT, ARTIST_TASK_COUNT));
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
