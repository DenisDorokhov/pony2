package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskSummary;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.dorokhov.pony2.core.library.service.discovery.ArtistDiscoveryService.ARTIST_TASK_COUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtistDiscoveryServiceTest {

    @InjectMocks
    private ArtistDiscoveryService artistDiscoveryService;

    @Mock
    @SuppressWarnings("unused")
    private AlbumRepository albumRepository;
    @Mock
    private DiscoveryTaskRepository discoveryTaskRepository;
    @Mock
    @SuppressWarnings("unused")
    private DiscoveryTaskResultReader discoveryTaskResultReader;
    @Mock
    private AlbumDiscoveryService albumDiscoveryService;
    @Mock
    private ArtistBioImageFacesService artistBioImageFacesService;
    @Mock
    private DiscoveryTaskExecutor taskExecutor;
    @Mock
    private LibraryJobSynchronizer jobSynchronizer;
    @Mock
    @SuppressWarnings("unused")
    private PlatformTransactionManager transactionManager;

    private final List<DiscoveryTaskType> executedTasks = new ArrayList<>();

    @BeforeEach
    void setUp() {
        lenient().when(discoveryTaskRepository.findByArtistId(anyString(), any(), eq(false), eq(false), any()))
                .thenReturn(Page.empty());
        lenient().when(discoveryTaskRepository.save(any(DiscoveryTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doAnswer(invocation -> {
            DiscoveryTaskExecution<?> execution = invocation.getArgument(0);
            executedTasks.add(execution.startTask().getType());
            return null;
        }).when(taskExecutor).execute(any());
    }

    @ParameterizedTest
    @CsvSource({"0,true,true,true,true", "1,false,true,true,true", "2,false,false,true,true",
            "3,false,false,true,false", "4,false,false,false,false"})
    public void shouldRetryOnlyMissingArtistTasks(long completedTasks, boolean artistRequired, boolean topTracksRequired,
                                                 boolean bioImageRequired, boolean avatarImageRequired) {
        DiscoveryJob job = discoveryJob();
        Artist artist = artist();
        artistTasks("artist1", completedTasks, LocalDateTime.now());

        artistDiscoveryService.discoverIfUpdated(job, artist, List.of(), true, null);

        assertThat(executedTasks).filteredOn(type -> type == DiscoveryTaskType.SPOTIFY_ARTIST_DATA)
                .hasSize(artistRequired ? 1 : 0);
        assertThat(executedTasks).filteredOn(type -> type == DiscoveryTaskType.SPOTIFY_TOP_TRACKS)
                .hasSize(topTracksRequired ? 1 : 0);
        assertThat(executedTasks).filteredOn(type -> type == DiscoveryTaskType.ARTIST_BIO_IMAGE)
                .hasSize(bioImageRequired ? 1 : 0);
        assertThat(executedTasks).filteredOn(type -> type == DiscoveryTaskType.ARTIST_AVATAR_IMAGE)
                .hasSize(avatarImageRequired ? 1 : 0);
    }

    @Test
    public void shouldDiscoverOutdatedArtistEvenWhenTasksAreComplete() {
        LocalDateTime now = LocalDateTime.now();
        DiscoveryJob job = discoveryJob();
        Artist artist = artist().setUpdateDate(now.plusMinutes(1));
        artistTasks("artist1", ARTIST_TASK_COUNT, now.plusMinutes(2));
        when(discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                "artist1", DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .thenReturn(Optional.of(now));

        artistDiscoveryService.discoverIfUpdated(job, artist, List.of(), true, null);

        assertThat(executedTasks).containsExactly(DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTaskType.SPOTIFY_TOP_TRACKS,
                DiscoveryTaskType.ARTIST_AVATAR_IMAGE, DiscoveryTaskType.ARTIST_BIO_IMAGE, DiscoveryTaskType.ARTIST_BIO_IMAGE_FACES);
    }

    @ParameterizedTest
    @EnumSource(value = DiscoveryTaskType.class, names = {"ARTIST_BIO_IMAGE", "ARTIST_AVATAR_IMAGE"})
    void shouldRediscoverOutdatedImageIndependently(DiscoveryTaskType type) {
        LocalDateTime now = LocalDateTime.now();
        Artist artist = artist().setUpdateDate(now);
        artistTasks(artist.getId(), ARTIST_TASK_COUNT, now.plusMinutes(1));
        when(discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(artist.getId(), type, Limit.of(1)))
                .thenReturn(Optional.of(now.minusMinutes(1)));

        artistDiscoveryService.discoverIfUpdated(discoveryJob(), artist, List.of(), true, null);

        assertThat(executedTasks).containsExactly(type);
    }

    @ParameterizedTest
    @CsvSource({"SPOTIFY_TOP_TRACKS,-1,false", "SPOTIFY_TOP_TRACKS,0,false", "SPOTIFY_TOP_TRACKS,1,true",
            "ARTIST_BIO_IMAGE,-1,false", "ARTIST_BIO_IMAGE,0,false", "ARTIST_BIO_IMAGE,1,true",
            "ARTIST_AVATAR_IMAGE,-1,false", "ARTIST_AVATAR_IMAGE,0,false", "ARTIST_AVATAR_IMAGE,1,true",
            "MUSICBRAINZ_ARTIST_DATA,-1,false", "MUSICBRAINZ_ARTIST_DATA,0,false", "MUSICBRAINZ_ARTIST_DATA,1,false"})
    void shouldRediscoverDependentTaskOnlyWhenSpotifyDataIsNewer(DiscoveryTaskType type, int minutesAfterImage, boolean required) {
        LocalDateTime imageDate = LocalDateTime.now();
        Artist artist = artist();
        artistTasks(artist.getId(), ARTIST_TASK_COUNT, imageDate.plusMinutes(2));
        when(discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                artist.getId(), DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .thenReturn(Optional.of(imageDate.plusMinutes(minutesAfterImage)));
        when(discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(artist.getId(), type, Limit.of(1)))
                .thenReturn(Optional.of(imageDate));

        artistDiscoveryService.discoverIfUpdated(discoveryJob(), artist, List.of(), true, null);

        assertThat(executedTasks).containsExactlyElementsOf(required ? List.of(type) : List.of());
    }

    @ParameterizedTest
    @CsvSource({"-1,false", "0,false", "1,true"})
    void shouldRediscoverTopTracksOnlyWhenMusicBrainzDataIsNewer(int minutesAfterTopTracks, boolean required) {
        LocalDateTime taskDate = LocalDateTime.now();
        Artist artist = artist();
        artistTasks(artist.getId(), ARTIST_TASK_COUNT, taskDate);
        when(discoveryTaskRepository.findLatestCompletedMusicBrainzCreationDateByArtistId(artist.getId()))
                .thenReturn(Optional.of(taskDate.plusMinutes(minutesAfterTopTracks)));

        artistDiscoveryService.discoverIfUpdated(discoveryJob(), artist, List.of(), true, null);

        assertThat(executedTasks).containsExactlyElementsOf(required ? List.of(DiscoveryTaskType.SPOTIFY_TOP_TRACKS) : List.of());
    }

    @Test
    void shouldCheckTopTracksFreshnessAfterAlbumDiscovery() {
        LocalDateTime taskDate = LocalDateTime.now();
        DiscoveryJob job = discoveryJob();
        Artist artist = artist();
        Album album = album(artist);
        artistTasks(artist.getId(), ARTIST_TASK_COUNT, taskDate);
        doAnswer(invocation -> {
            executedTasks.add(DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA);
            when(discoveryTaskRepository.findLatestCompletedMusicBrainzCreationDateByArtistId(artist.getId()))
                    .thenReturn(Optional.of(taskDate.plusMinutes(1)));
            return null;
        }).when(albumDiscoveryService).discover(job, album, true, null);

        artistDiscoveryService.discoverIfUpdated(job, artist, List.of(album), true, null);

        assertThat(executedTasks).containsExactly(DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTaskType.SPOTIFY_TOP_TRACKS,
                DiscoveryTaskType.MUSICBRAINZ_ARTIST_DATA);
    }

    @Test
    void shouldDiscoverMusicBrainzArtistAfterTopTracksAndBeforeImages() {
        DiscoveryJob job = discoveryJob();
        Artist artist = artist();
        Album album = album(artist);
        doAnswer(invocation -> {
            executedTasks.add(DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA);
            return null;
        }).when(albumDiscoveryService).discover(job, album, true, null);

        artistDiscoveryService.discoverIfUpdated(job, artist, List.of(album), true, null);

        assertThat(executedTasks).containsExactly(DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA,
                DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTaskType.MUSICBRAINZ_ARTIST_DATA, DiscoveryTaskType.ARTIST_AVATAR_IMAGE,
                DiscoveryTaskType.ARTIST_BIO_IMAGE, DiscoveryTaskType.ARTIST_BIO_IMAGE_FACES);
    }

    @ParameterizedTest
    @CsvSource({
            "SPOTIFY_ARTIST_DATA,-1,true", "SPOTIFY_ARTIST_DATA,0,false", "SPOTIFY_ARTIST_DATA,1,false",
            "SPOTIFY_TOP_TRACKS,-1,true", "SPOTIFY_TOP_TRACKS,0,false", "SPOTIFY_TOP_TRACKS,1,false",
            "ARTIST_BIO_IMAGE,-1,true", "ARTIST_BIO_IMAGE,0,false", "ARTIST_BIO_IMAGE,1,false",
            "ARTIST_AVATAR_IMAGE,-1,true", "ARTIST_AVATAR_IMAGE,0,false", "ARTIST_AVATAR_IMAGE,1,false"
    })
    public void shouldDiscoverArtistTaskOnlyWhenInputIsNewer(DiscoveryTaskType type, int minutesAfterInput,
                                                             boolean required) {
        LocalDateTime inputDate = LocalDateTime.now().plusDays(1);
        DiscoveryJob job = discoveryJob();
        Artist artist = artist().setCreationDate(inputDate.minusDays(1)).setUpdateDate(inputDate);
        artistTasks(artist.getId(), ARTIST_TASK_COUNT, inputDate.plusMinutes(2));
        when(discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                artist.getId(), DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .thenReturn(Optional.of(inputDate));
        when(discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(artist.getId(), type, Limit.of(1)))
                .thenReturn(Optional.of(inputDate.plusMinutes(minutesAfterInput)));

        artistDiscoveryService.discoverIfUpdated(job, artist, List.of(), true, null);

        List<DiscoveryTaskType> expectedTasks;
        if (!required) {
            expectedTasks = List.of();
        } else if (type == DiscoveryTaskType.SPOTIFY_ARTIST_DATA) {
            expectedTasks = List.of(DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTaskType.SPOTIFY_TOP_TRACKS,
                    DiscoveryTaskType.ARTIST_AVATAR_IMAGE, DiscoveryTaskType.ARTIST_BIO_IMAGE, DiscoveryTaskType.ARTIST_BIO_IMAGE_FACES);
        } else {
            expectedTasks = List.of(type);
        }
        assertThat(executedTasks).containsExactlyElementsOf(expectedTasks);
    }

    @ParameterizedTest
    @CsvSource({"false,0,false,true", "true,0,false,true", "true,1,false,false", "true,1,true,true"})
    public void shouldCheckAlbumsIndependentlyOfCompletedArtist(boolean exists, long completedTasks,
                                                               boolean outdated, boolean required) {
        LocalDateTime taskDate = DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA.getImplementedAt().orElse(Instant.EPOCH)
                .atZone(ZoneId.systemDefault()).toLocalDateTime().plusDays(1);
        DiscoveryJob job = discoveryJob();
        Artist artist = artist().setCreationDate(taskDate.minusDays(1));
        Album album = album(artist);
        if (outdated) {
            album.setUpdateDate(taskDate.plusMinutes(1));
        }
        artistTasks("artist1", ARTIST_TASK_COUNT, taskDate);
        if (exists) {
            albumTasks("album1", completedTasks, taskDate);
        }

        artistDiscoveryService.discoverIfUpdated(job, artist, List.of(album), true, null);

        verify(albumDiscoveryService, times(required ? 1 : 0)).discover(job, album, true, null);
        assertThat(executedTasks).containsExactlyElementsOf(required ? List.of(DiscoveryTaskType.MUSICBRAINZ_ARTIST_DATA) : List.of());
    }

    @ParameterizedTest
    @CsvSource({"-1,true", "0,false", "1,false"})
    public void shouldDiscoverAlbumOnlyWhenInputIsNewer(int minutesAfterInput, boolean required) {
        LocalDateTime inputDate = DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA.getImplementedAt().orElse(Instant.EPOCH)
                .atZone(ZoneId.systemDefault()).toLocalDateTime().plusDays(1);
        DiscoveryJob job = discoveryJob();
        Artist artist = artist().setCreationDate(inputDate.minusDays(1));
        Album album = album(artist).setUpdateDate(inputDate);
        artistTasks(artist.getId(), ARTIST_TASK_COUNT, inputDate.plusMinutes(2));
        albumTasks(album.getId(), 1, inputDate.plusMinutes(minutesAfterInput));

        artistDiscoveryService.discoverIfUpdated(job, artist, List.of(album), true, null);

        verify(albumDiscoveryService, times(required ? 1 : 0)).discover(job, album, true, null);
        assertThat(executedTasks).containsExactlyElementsOf(required ? List.of(DiscoveryTaskType.MUSICBRAINZ_ARTIST_DATA) : List.of());
    }

    @Test
    public void shouldStopBeforeIncompleteAlbumWhenArtistIsComplete() {
        DiscoveryJob job = discoveryJob();
        Artist artist = artist();
        Album album = album(artist);
        artistTasks("artist1", ARTIST_TASK_COUNT, LocalDateTime.now());
        doThrow(new DiscoveryInterruptedException()).when(jobSynchronizer).interruptDiscoveryIfCancelled();

        assertThatThrownBy(() -> artistDiscoveryService.discoverIfUpdated(job, artist, List.of(album), true, null))
                .isInstanceOf(DiscoveryInterruptedException.class);

        verify(discoveryTaskRepository, never()).findLatestCompletedCreationDateByAlbumIdAndType(any(), any(), any());
    }

    @ParameterizedTest
    @CsvSource({"-1,false", "0,false", "1,true"})
    void shouldRecheckMusicBrainzArtistAfterNewerAlbumAttempt(int minutesAfterCheck, boolean required) {
        LocalDateTime taskDate = LocalDateTime.now();
        Artist artist = artist();
        artistTasks(artist.getId(), ARTIST_TASK_COUNT, taskDate);
        when(discoveryTaskRepository.findByArtistId(artist.getId(), DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA,
                false, false, PageRequest.of(0, 1))).thenReturn(new PageImpl<>(List.of(
                DiscoveryTaskSummary.of(new DiscoveryTask().setCreationDate(taskDate.plusMinutes(minutesAfterCheck))))));

        artistDiscoveryService.discoverIfUpdated(discoveryJob(), artist, List.of(), true, null);

        assertThat(executedTasks).containsExactlyElementsOf(required ? List.of(DiscoveryTaskType.MUSICBRAINZ_ARTIST_DATA) : List.of());
    }

    @Test
    void shouldDiscoverMusicBrainzArtistWithoutCompletedResult() {
        Artist artist = artist();
        artistTasks(artist.getId(), ARTIST_TASK_COUNT, LocalDateTime.now());
        when(discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                artist.getId(), DiscoveryTaskType.MUSICBRAINZ_ARTIST_DATA, Limit.of(1))).thenReturn(Optional.empty());

        artistDiscoveryService.discoverIfUpdated(discoveryJob(), artist, List.of(), true, null);

        assertThat(executedTasks).containsExactly(DiscoveryTaskType.MUSICBRAINZ_ARTIST_DATA);
    }

    @Test
    void shouldRebuildAlbumResultsAfterImplementationUpdate() {
        LocalDateTime implementedAt = DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA.getImplementedAt().orElseThrow()
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
        Artist artist = artist().setCreationDate(implementedAt.minusDays(1));
        Album album = album(artist);
        DiscoveryJob job = discoveryJob();
        artistTasks(artist.getId(), ARTIST_TASK_COUNT, implementedAt.plusMinutes(1));
        albumTasks(album.getId(), 1, implementedAt.minusSeconds(1));

        artistDiscoveryService.discoverIfUpdated(job, artist, List.of(album), true, null);

        verify(albumDiscoveryService).discover(job, album, true, null);
        assertThat(executedTasks).containsExactly(DiscoveryTaskType.MUSICBRAINZ_ARTIST_DATA);
    }

    private void artistTasks(String artistId, long count, LocalDateTime date) {
        lenient().when(discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                artistId, DiscoveryTaskType.MUSICBRAINZ_ARTIST_DATA, Limit.of(1))).thenReturn(count >= ARTIST_TASK_COUNT
                ? Optional.of(date) : Optional.empty());
        List<DiscoveryTaskType> types = List.of(DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTaskType.SPOTIFY_TOP_TRACKS,
                DiscoveryTaskType.ARTIST_AVATAR_IMAGE, DiscoveryTaskType.ARTIST_BIO_IMAGE);
        for (int i = 0; i < types.size(); i++) {
            lenient().when(discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                    artistId, types.get(i), Limit.of(1)))
                    .thenReturn(count > i ? Optional.of(date) : Optional.empty());
        }
    }

    private void albumTasks(String albumId, long count, LocalDateTime date) {
        lenient().when(discoveryTaskRepository.findLatestCompletedCreationDateByAlbumIdAndType(
                albumId, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, Limit.of(1)))
                .thenReturn(count >= 1 ? Optional.of(date) : Optional.empty());
    }

    private DiscoveryJob discoveryJob() {
        return new DiscoveryJob().setType(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.STARTED);
    }

    private Artist artist() {
        return new Artist().setId("artist1").setName("Artist").setCreationDate(LocalDateTime.now().minusDays(1));
    }

    private Album album(Artist artist) {
        return new Album().setId("album1").setArtist(artist).setName("album1").setCreationDate(artist.getCreationDate());
    }
}
