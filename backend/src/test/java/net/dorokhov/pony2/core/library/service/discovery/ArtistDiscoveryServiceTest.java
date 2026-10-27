package net.dorokhov.pony2.core.library.service.discovery;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.api.library.domain.DiscoveryType;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtistDiscoveryServiceTest {

    @InjectMocks
    private ArtistDiscoveryService artistDiscoveryService;

    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private DiscoveryTaskRepository discoveryTaskRepository;
    @Mock
    private AlbumDiscoveryService albumDiscoveryService;
    @Mock
    private DiscoveryTaskExecutor taskExecutor;
    @Mock
    private LibraryJobSynchronizer jobSynchronizer;
    @Mock
    private PlatformTransactionManager transactionManager;

    private final List<DiscoveryTaskType> executedTasks = new ArrayList<>();

    @BeforeEach
    void setUp() {
        lenient().when(discoveryTaskRepository.save(any(DiscoveryTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doAnswer(invocation -> {
            DiscoveryTaskExecution<?> execution = invocation.getArgument(0);
            executedTasks.add(execution.startTask().getType());
            return null;
        }).when(taskExecutor).execute(any());
    }

    @ParameterizedTest
    @CsvSource({"0,true,true", "1,false,true", "2,false,false"})
    public void shouldRetryOnlyMissingArtistTasks(long completedTasks, boolean artistRequired, boolean topTracksRequired) {
        DiscoveryJob job = discoveryJob();
        Artist artist = artist("artist1");
        artistTasks("artist1", completedTasks, LocalDateTime.now());

        artistDiscoveryService.discoverIfUpdated(job, artist, List.of(), true, null);

        assertThat(executedTasks).filteredOn(type -> type == DiscoveryTaskType.SPOTIFY_ARTIST_DATA)
                .hasSize(artistRequired ? 1 : 0);
        assertThat(executedTasks).filteredOn(type -> type == DiscoveryTaskType.SPOTIFY_TOP_TRACKS)
                .hasSize(topTracksRequired ? 1 : 0);
    }

    @Test
    public void shouldDiscoverOutdatedArtistEvenWhenTasksAreComplete() {
        LocalDateTime now = LocalDateTime.now();
        DiscoveryJob job = discoveryJob();
        Artist artist = artist("artist1").setUpdateDate(now.plusMinutes(1));
        artistTasks("artist1", 2, now.plusMinutes(2));
        when(discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                "artist1", DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .thenReturn(Optional.of(now));

        artistDiscoveryService.discoverIfUpdated(job, artist, List.of(), true, null);

        assertThat(executedTasks).containsExactly(DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTaskType.SPOTIFY_TOP_TRACKS);
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
        artistTasks(artist.getId(), 2, implementedAt.plusMinutes(1));
        when(discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(artist.getId(), type, Limit.of(1)))
                .thenReturn(Optional.of(taskDate));
        Logger logger = (Logger) LoggerFactory.getLogger(ArtistDiscoveryService.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        try {
            artistDiscoveryService.discoverIfUpdated(job, artist, List.of(), true, null);

            assertThat(executedTasks).containsExactlyElementsOf(required ? List.of(DiscoveryTaskType.SPOTIFY_TOP_TRACKS) : List.of());
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
        artistTasks("artist1", 2L, now);
        if (exists) {
            albumTasks("album1", completedTasks, now);
        }

        artistDiscoveryService.discoverIfUpdated(job, artist, List.of(album), true, null);

        verify(albumDiscoveryService, times(required ? 1 : 0)).discover(job, album, true, null);
        assertThat(executedTasks).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"-1,true", "0,false", "1,false"})
    public void shouldRediscoverAlbumOnlyWhenTaskImplementationIsNewer(int minutesAfterUpdate, boolean required) {
        LocalDateTime implementedAt = DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA.getImplementedAt().orElseThrow();
        DiscoveryJob job = discoveryJob();
        Artist artist = artist("artist1").setCreationDate(implementedAt.minusDays(1));
        Album album = album("album1", artist);
        artistTasks(artist.getId(), 2, LocalDateTime.now());
        albumTasks(album.getId(), 1, implementedAt.plusMinutes(minutesAfterUpdate));

        artistDiscoveryService.discoverIfUpdated(job, artist, List.of(album), true, null);

        verify(albumDiscoveryService, times(required ? 1 : 0)).discover(job, album, true, null);
        assertThat(executedTasks).isEmpty();
    }

    @Test
    public void shouldStopBeforeIncompleteAlbumWhenArtistIsComplete() {
        DiscoveryJob job = discoveryJob();
        Artist artist = artist("artist1");
        Album album = album("album1", artist);
        artistTasks("artist1", 2L, LocalDateTime.now());
        doThrow(new DiscoveryInterruptedException()).when(jobSynchronizer).interruptDiscoveryIfCancelled();

        assertThatThrownBy(() -> artistDiscoveryService.discoverIfUpdated(job, artist, List.of(album), true, null))
                .isInstanceOf(DiscoveryInterruptedException.class);

        verify(discoveryTaskRepository, never()).findLatestCompletedCreationDateByAlbumIdAndType(any(), any(), any());
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
