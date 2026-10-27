package net.dorokhov.pony2.core.library.service.discovery;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Limit;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
    private DiscoveryTaskRepository discoveryTaskRepository;
    @Mock
    private ArtistDiscoveryService artistDiscoveryService;
    @Mock
    private LibraryJobSynchronizer.DiscoveryTaskRegistration taskRegistration;
    @Test
    public void shouldDiscoverArtistWhenDiscoveryIsMissing() {
        DiscoveryJob job = discoveryJob();
        Artist artist = artist("artist1");
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));

        fullDiscoveryService.discover(job, false, null);

        verify(artistRepository).findAll(Sort.by("id"));
        verify(artistDiscoveryService).discoverForFullJob(eq(job), eq(artist), eq(false),
                eq(List.of()), eq(true), eq(true), any(), isNull());
        verify(jobSynchronizer, times(2)).interruptDiscoveryIfCancelled();
    }

    @ParameterizedTest
    @CsvSource({"0,true,true", "1,false,true", "2,false,false"})
    public void shouldRetryOnlyMissingArtistTasks(long completedTasks, boolean artistRequired, boolean topTracksRequired) {
        DiscoveryJob job = discoveryJob();
        Artist artist = artist("artist1");
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        artistTasks("artist1", completedTasks, LocalDateTime.now());

        fullDiscoveryService.discover(job, null);

        verify(artistDiscoveryService).discoverForFullJob(eq(job), eq(artist), eq(true),
                eq(List.of()), eq(artistRequired), eq(topTracksRequired), any(), isNull());
        verify(jobSynchronizer, times(2)).interruptDiscoveryIfCancelled();
    }

    @Test
    public void shouldDiscoverOutdatedArtistEvenWhenTasksAreComplete() {
        LocalDateTime now = LocalDateTime.now();
        DiscoveryJob job = discoveryJob();
        Artist artist = artist("artist1").setUpdateDate(now.plusMinutes(1));
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        artistTasks("artist1", 2, now.plusMinutes(2));
        when(discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                "artist1", DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .thenReturn(Optional.of(now));

        fullDiscoveryService.discover(job, null);

        verify(artistDiscoveryService).discoverForFullJob(eq(job), eq(artist), eq(true),
                eq(List.of()), eq(true), eq(true), any(), isNull());
    }

    @ParameterizedTest
    @CsvSource({
            "SPOTIFY_ARTIST_DATA,-1,false", "SPOTIFY_ARTIST_DATA,0,false", "SPOTIFY_ARTIST_DATA,1,false",
            "SPOTIFY_TOP_TRACKS,-1,true", "SPOTIFY_TOP_TRACKS,0,false", "SPOTIFY_TOP_TRACKS,1,false"
    })
    public void shouldRediscoverOnlyTaskWhoseImplementationIsNewer(DiscoveryTaskType type, int minutesAfterUpdate,
                                                                  boolean required) {
        LocalDateTime implementedAt = DiscoveryTaskType.SPOTIFY_TOP_TRACKS.getImplementedAt().orElseThrow();
        LocalDateTime taskDate = implementedAt.plusMinutes(minutesAfterUpdate);
        DiscoveryJob job = discoveryJob();
        Artist artist = artist("artist1").setCreationDate(implementedAt.minusDays(1));
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        artistTasks(artist.getId(), 2, implementedAt.plusMinutes(1));
        when(discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(artist.getId(), type, Limit.of(1)))
                .thenReturn(Optional.of(taskDate));
        Logger logger = (Logger) LoggerFactory.getLogger(FullDiscoveryService.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        try {
            fullDiscoveryService.discover(job, null);

            verify(artistDiscoveryService).discoverForFullJob(eq(job), eq(artist), eq(true),
                    eq(List.of()), eq(false), eq(required), any(), isNull());
            assertThat(appender.list).hasSize(required ? 1 : 0);
            if (required) {
                assertThat(appender.list).singleElement().satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
                    assertThat(event.getArgumentArray()).containsExactly(type, artist.getId(), implementedAt, taskDate);
                });
            }
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
    }

    @ParameterizedTest
    @CsvSource({"false,0,false,true", "true,0,false,true", "true,1,false,false", "true,1,true,true"})
    public void shouldCheckAlbumsIndependentlyOfCompletedArtist(boolean exists, long completedTasks,
                                                               boolean outdated, boolean required) {
        LocalDateTime now = LocalDateTime.now();
        DiscoveryJob job = discoveryJob();
        Artist artist = artist("artist1");
        Album album = album("album1", artist);
        if (outdated) {
            album.setUpdateDate(now.plusMinutes(1));
        }
        List<Album> albums = List.of(album);
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        when(albumRepository.count()).thenReturn(1L);
        when(albumRepository.findByArtistId("artist1")).thenReturn(albums);
        artistTasks("artist1", 2L, now);
        if (exists) {
            albumTasks("album1", completedTasks, now);
        }
        doAnswer(invocation -> {
            Predicate<Album> predicate = invocation.getArgument(6);
            assertThat(predicate.test(album)).isEqualTo(required);
            return null;
        }).when(artistDiscoveryService).discoverForFullJob(eq(job), eq(artist), eq(true),
                eq(albums), eq(false), eq(false), any(), isNull());

        fullDiscoveryService.discover(job, null);

        verify(jobSynchronizer, times(3)).interruptDiscoveryIfCancelled();
    }

    @ParameterizedTest
    @CsvSource({"-1,true", "0,false", "1,false"})
    public void shouldRediscoverAlbumOnlyWhenTaskImplementationIsNewer(int minutesAfterUpdate, boolean required) {
        LocalDateTime implementedAt = DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA.getImplementedAt().orElseThrow();
        DiscoveryJob job = discoveryJob();
        Artist artist = artist("artist1").setCreationDate(implementedAt.minusDays(1));
        Album album = album("album1", artist);
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        when(albumRepository.findByArtistId(artist.getId())).thenReturn(List.of(album));
        artistTasks(artist.getId(), 2, LocalDateTime.now());
        albumTasks(album.getId(), 1, implementedAt.plusMinutes(minutesAfterUpdate));
        doAnswer(invocation -> {
            Predicate<Album> predicate = invocation.getArgument(6);
            assertThat(predicate.test(album)).isEqualTo(required);
            return null;
        }).when(artistDiscoveryService).discoverForFullJob(eq(job), eq(artist), eq(true),
                eq(List.of(album)), eq(false), eq(false), any(), isNull());

        fullDiscoveryService.discover(job, null);

        verify(artistDiscoveryService).discoverForFullJob(eq(job), eq(artist), eq(true),
                eq(List.of(album)), eq(false), eq(false), any(), isNull());
    }

    @Test
    public void shouldReportEmptyLibraryProgress() {
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of());
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(discoveryJob(), progressCalls::add);

        assertThat(progressCalls).extracting(DiscoveryProgress::getValue)
                .containsExactly(DiscoveryProgress.Value.of(0, 0));
        verifyNoInteractions(discoveryTaskRepository, artistDiscoveryService);
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
        artistTasks("artist1", 0, LocalDateTime.now());
        artistTasks("artist2", 2L, LocalDateTime.now());
        doAnswer(invocation -> {
            Consumer<DiscoveryProgress> observer = invocation.getArgument(7);
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 4)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(1, 4)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(4, 4)));
            return null;
        }).when(artistDiscoveryService).discoverForFullJob(eq(job), eq(first), eq(true),
                eq(firstAlbums), eq(true), eq(true), any(), any());
        doAnswer(invocation -> {
            Consumer<DiscoveryProgress> observer = invocation.getArgument(7);
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(2, 3)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(3, 3)));
            return null;
        }).when(artistDiscoveryService).discoverForFullJob(eq(job), eq(second), eq(true),
                eq(secondAlbums), eq(false), eq(false), any(), any());
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
        verifyNoInteractions(artistDiscoveryService, discoveryTaskRepository);
        verify(albumRepository, never()).findByArtistId(any());
        verify(taskRegistration).close();
    }

    @Test
    public void shouldStopBeforeIncompleteAlbumWhenArtistIsComplete() {
        DiscoveryJob job = discoveryJob();
        Artist artist = artist("artist1");
        Album album = album("album1", artist);
        when(jobSynchronizer.registerDiscoveryTask()).thenReturn(taskRegistration);
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        when(albumRepository.findByArtistId("artist1")).thenReturn(List.of(album));
        artistTasks("artist1", 2L, LocalDateTime.now());
        doNothing().doThrow(new DiscoveryInterruptedException()).when(jobSynchronizer).interruptDiscoveryIfCancelled();
        doAnswer(invocation -> {
            Predicate<Album> predicate = invocation.getArgument(6);
            predicate.test(album);
            return null;
        }).when(artistDiscoveryService).discoverForFullJob(eq(job), eq(artist), eq(true),
                eq(List.of(album)), eq(false), eq(false), any(), isNull());

        assertThatThrownBy(() -> fullDiscoveryService.discover(job, null))
                .isInstanceOf(DiscoveryInterruptedException.class);

        verify(discoveryTaskRepository, never()).findLatestCompletedCreationDateByAlbumIdAndType(any(), any(), any());
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
            Consumer<DiscoveryProgress> observer = invocation.getArgument(7);
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(1, 2)));
            throw new DiscoveryInterruptedException();
        }).when(artistDiscoveryService).discoverForFullJob(eq(job), eq(first), eq(true),
                eq(List.of()), eq(true), eq(true), any(), any());
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        assertThatThrownBy(() -> fullDiscoveryService.discover(job, progressCalls::add))
                .isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(progressCalls).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, 4), DiscoveryProgress.Value.of(1, 4));
        verify(discoveryTaskRepository, never()).findLatestCompletedCreationDateByArtistIdAndType(eq("artist2"), any(), any());
        verify(taskRegistration).close();
    }

    @Test
    public void shouldHandleUnknownAndEmptyArtistProgress() {
        DiscoveryJob job = discoveryJob();
        Artist artist = artist("artist1");
        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        doAnswer(invocation -> {
            Consumer<DiscoveryProgress> observer = invocation.getArgument(7);
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, null));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 0)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(1, 2)));
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(2, 2)));
            return null;
        }).when(artistDiscoveryService).discoverForFullJob(eq(job), eq(artist), eq(true),
                eq(List.of()), eq(true), eq(true), any(), any());
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
        verify(artistDiscoveryService).discoverForFullJob(eq(job), eq(artist), eq(true),
                eq(List.of()), eq(true), eq(true), any(), any());
    }

    private void artistTasks(String artistId, long count, LocalDateTime date) {
        lenient().when(discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                artistId, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .thenReturn(count >= 1 ? Optional.of(date) : Optional.empty());
        lenient().when(discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                artistId, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, Limit.of(1)))
                .thenReturn(count >= 2 ? Optional.of(date) : Optional.empty());
    }

    private void albumTasks(String albumId, long count, LocalDateTime date) {
        lenient().when(discoveryTaskRepository.findLatestCompletedCreationDateByAlbumIdAndType(
                albumId, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, Limit.of(1)))
                .thenReturn(count >= 1 ? Optional.of(date) : Optional.empty());
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
