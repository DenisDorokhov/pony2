package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.api.library.domain.DiscoveryType;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.ARTIST_PROCESSING;
import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.FULL_PLANNING;
import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.FULL_PROCESSING;
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
    public void shouldDelegatePlannedDiscoveryWithoutObserverOrCache() {
        DiscoveryJob job = discoveryJob();
        Artist artist = artist("artist1");
        ArtistDiscoveryService.DiscoveryPlan plan = plan(artist, ARTIST_TASK_COUNT, DiscoveryTaskType.ARTIST_AVATAR_IMAGE);
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        when(artistDiscoveryService.plan(artist, List.of())).thenReturn(plan);

        fullDiscoveryService.discover(job, false, null);

        verify(artistRepository).findAll(Sort.by("id"));
        verify(artistDiscoveryService).discover(job, plan, false, null);
    }

    @Test
    public void shouldReportPlanningAndEmptyLibraryProgress() {
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of());
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(discoveryJob(), progressCalls::add);

        assertThat(progressCalls).extracting(DiscoveryProgress::getStep).containsExactly(FULL_PLANNING, FULL_PROCESSING);
        assertThat(progressCalls).extracting(DiscoveryProgress::getValue).containsExactly(null, DiscoveryProgress.Value.of(0, 0));
        verifyNoInteractions(artistDiscoveryService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldReserveLastTwoPercentRegardlessOfRequiredArtistOrder(boolean requiredArtistLast) {
        DiscoveryJob job = discoveryJob();
        List<Artist> artists = new ArrayList<>();
        List<DiscoveryProgress> progressCalls = new ArrayList<>();
        for (int index = 0; index < 16; index++) {
            Artist artist = artist(String.format("artist%02d", index));
            artists.add(artist);
            List<Album> albums = index == 0 ? List.of(new Album().setArtist(artist), new Album().setArtist(artist),
                    new Album().setArtist(artist), new Album().setArtist(artist)) : List.of();
            when(albumRepository.findByArtistId(artist.getId())).thenReturn(albums);
            boolean required = index == (requiredArtistLast ? 15 : 0);
            ArtistDiscoveryService.DiscoveryPlan plan = plan(artist, ARTIST_TASK_COUNT + albums.size(),
                    required ? new DiscoveryTaskType[]{DiscoveryTaskType.ARTIST_AVATAR_IMAGE, DiscoveryTaskType.SPOTIFY_TOP_TRACKS}
                            : new DiscoveryTaskType[]{});
            when(artistDiscoveryService.plan(artist, albums)).thenAnswer(invocation -> {
                assertThat(progressCalls).extracting(DiscoveryProgress::getStep).containsOnly(FULL_PLANNING);
                return plan;
            });
            if (required) {
                doAnswer(invocation -> {
                    for (Artist plannedArtist : artists) {
                        verify(artistDiscoveryService).plan(eq(plannedArtist), anyList());
                    }
                    Consumer<DiscoveryProgress> observer = invocation.getArgument(3);
                    observer.accept(new DiscoveryProgress(ARTIST_PROCESSING, DiscoveryProgress.Value.of(plan.tasksSkipped(), plan.tasksTotal())));
                    observer.accept(new DiscoveryProgress(ARTIST_PROCESSING, DiscoveryProgress.Value.of(plan.tasksSkipped() + 1, plan.tasksTotal())));
                    observer.accept(new DiscoveryProgress(ARTIST_PROCESSING, DiscoveryProgress.Value.of(plan.tasksTotal(), plan.tasksTotal())));
                    return null;
                }).when(artistDiscoveryService).discover(eq(job), eq(plan), eq(true), any());
            }
        }
        when(artistRepository.findAll(any(Sort.class))).thenReturn(artists);

        fullDiscoveryService.discover(job, progressCalls::add);

        assertThat(progressCalls).extracting(DiscoveryProgress::getValue).containsExactly(null,
                DiscoveryProgress.Value.of(98, 100), DiscoveryProgress.Value.of(98, 100),
                DiscoveryProgress.Value.of(99, 100), DiscoveryProgress.Value.of(100, 100), DiscoveryProgress.Value.of(100, 100));
        verify(artistDiscoveryService, times(1)).discover(eq(job), any(ArtistDiscoveryService.DiscoveryPlan.class), eq(true), any());
    }

    @Test
    public void shouldAggregateRemainingTasksAcrossArtists() {
        DiscoveryJob job = discoveryJob();
        Artist first = artist("artist1");
        Artist second = artist("artist2");
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(first, second));
        for (Artist artist : List.of(first, second)) {
            ArtistDiscoveryService.DiscoveryPlan plan = plan(artist, ARTIST_TASK_COUNT, DiscoveryTaskType.ARTIST_AVATAR_IMAGE);
            when(artistDiscoveryService.plan(artist, List.of())).thenReturn(plan);
            doAnswer(invocation -> {
                Consumer<DiscoveryProgress> observer = invocation.getArgument(3);
                observer.accept(new DiscoveryProgress(ARTIST_PROCESSING, DiscoveryProgress.Value.of(ARTIST_TASK_COUNT, ARTIST_TASK_COUNT)));
                return null;
            }).when(artistDiscoveryService).discover(eq(job), eq(plan), eq(true), any());
        }
        List<DiscoveryProgress> progressCalls = new ArrayList<>();
        long total = 2L * ARTIST_TASK_COUNT;

        fullDiscoveryService.discover(job, progressCalls::add);

        assertThat(progressCalls).extracting(DiscoveryProgress::getValue).containsExactly(null,
                DiscoveryProgress.Value.of(total - 2, total), DiscoveryProgress.Value.of(total - 1, total),
                DiscoveryProgress.Value.of(total - 1, total), DiscoveryProgress.Value.of(total, total), DiscoveryProgress.Value.of(total, total));
    }

    @Test
    public void shouldCompleteWithoutExecutingUpToDateTasks() {
        Artist artist = artist("artist1");
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        when(artistDiscoveryService.plan(artist, List.of())).thenReturn(plan(artist, ARTIST_TASK_COUNT));
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(discoveryJob(), progressCalls::add);

        assertThat(progressCalls).extracting(DiscoveryProgress::getValue)
                .containsExactly(null, DiscoveryProgress.Value.of(ARTIST_TASK_COUNT, ARTIST_TASK_COUNT));
        verify(artistDiscoveryService, never()).discover(any(), any(ArtistDiscoveryService.DiscoveryPlan.class), anyBoolean(), any());
    }

    @Test
    public void shouldStopBeforePlanningWhenCancelled() {
        when(jobSynchronizer.registerDiscoveryTask()).thenReturn(taskRegistration);
        doThrow(new DiscoveryInterruptedException()).when(jobSynchronizer).interruptDiscoveryIfCancelled();
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        assertThatThrownBy(() -> fullDiscoveryService.discover(discoveryJob(), progressCalls::add))
                .isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(progressCalls).extracting(DiscoveryProgress::getStep).containsExactly(FULL_PLANNING);
        verifyNoInteractions(artistDiscoveryService, artistRepository, albumRepository);
        verify(taskRegistration).close();
    }

    @Test
    public void shouldStopPlanningBeforeNextArtistWhenCancelled() {
        Artist first = artist("artist1");
        Artist second = artist("artist2");
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(first, second));
        when(artistDiscoveryService.plan(first, List.of())).thenAnswer(invocation -> {
            doThrow(new DiscoveryInterruptedException()).when(jobSynchronizer).interruptDiscoveryIfCancelled();
            return plan(first, ARTIST_TASK_COUNT);
        });
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        assertThatThrownBy(() -> fullDiscoveryService.discover(discoveryJob(), progressCalls::add))
                .isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(progressCalls).extracting(DiscoveryProgress::getStep).containsExactly(FULL_PLANNING);
        verify(artistDiscoveryService, never()).plan(eq(second), anyList());
        verify(artistDiscoveryService, never()).discover(any(), any(ArtistDiscoveryService.DiscoveryPlan.class), anyBoolean(), any());
    }

    @Test
    public void shouldStopWhenArtistDiscoveryIsInterrupted() {
        DiscoveryJob job = discoveryJob();
        Artist first = artist("artist1");
        Artist second = artist("artist2");
        ArtistDiscoveryService.DiscoveryPlan firstPlan = plan(first, ARTIST_TASK_COUNT, DiscoveryTaskType.ARTIST_AVATAR_IMAGE);
        ArtistDiscoveryService.DiscoveryPlan secondPlan = plan(second, ARTIST_TASK_COUNT, DiscoveryTaskType.ARTIST_AVATAR_IMAGE);
        when(jobSynchronizer.registerDiscoveryTask()).thenReturn(taskRegistration);
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(first, second));
        when(artistDiscoveryService.plan(first, List.of())).thenReturn(firstPlan);
        when(artistDiscoveryService.plan(second, List.of())).thenReturn(secondPlan);
        doThrow(new DiscoveryInterruptedException()).when(artistDiscoveryService).discover(eq(job), eq(firstPlan), eq(true), any());
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        assertThatThrownBy(() -> fullDiscoveryService.discover(job, progressCalls::add))
                .isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(progressCalls).extracting(DiscoveryProgress::getValue).containsExactly(null,
                DiscoveryProgress.Value.of(2L * ARTIST_TASK_COUNT - 2, 2L * ARTIST_TASK_COUNT));
        verify(artistDiscoveryService, never()).discover(eq(job), eq(secondPlan), anyBoolean(), any());
        verify(taskRegistration).close();
    }

    private ArtistDiscoveryService.DiscoveryPlan plan(Artist artist, long tasksTotal, DiscoveryTaskType... types) {
        EnumSet<DiscoveryTaskType> taskTypes = EnumSet.noneOf(DiscoveryTaskType.class);
        taskTypes.addAll(List.of(types));
        return new ArtistDiscoveryService.DiscoveryPlan(artist, List.of(), taskTypes, tasksTotal);
    }

    private DiscoveryJob discoveryJob() {
        return new DiscoveryJob().setType(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.STARTED);
    }

    private Artist artist(String id) {
        return new Artist().setId(id).setName(id).setCreationDate(LocalDateTime.now().minusDays(1));
    }
}
