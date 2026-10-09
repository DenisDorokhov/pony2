package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.repository.AlbumDiscoveryRepository;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistDiscoveryRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.FULL_ALBUM_DISCOVERY;
import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.FULL_ARTIST_DISCOVERY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private AlbumRepository albumRepository;
    @Mock
    private ArtistDiscoveryRepository artistDiscoveryRepository;
    @Mock
    private AlbumDiscoveryRepository albumDiscoveryRepository;

    @Test
    public void shouldDiscoverArtistsAndAlbums() {

        DiscoveryJob discoveryJob = discoveryJob();
        Artist artist1 = artist("artist1");
        Artist artist2 = artist("artist2");
        Album album1 = album("album1");
        Album album2 = album("album2");

        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist1, artist2));
        when(albumRepository.findAll((Pageable) any())).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(0);
            Pageable page = PageRequest.of(pageable.getPageNumber(), 1, pageable.getSort());
            if (pageable.getPageNumber() == 0) {
                return new PageImpl<>(List.of(album1), page, 2);
            } else {
                return new PageImpl<>(List.of(album2), page, 2);
            }
        });
        when(artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc(any())).thenReturn(Optional.empty());
        when(albumDiscoveryRepository.findFirstByAlbumIdOrderByCreationDateDesc(any())).thenReturn(Optional.empty());

        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(discoveryJob, progressCalls::add);

        verify(artistRepository).findAll(Sort.by("id"));
        verify(artistDiscoveryRepository).findFirstByArtistIdOrderByCreationDateDesc("artist1");
        verify(artistDiscoveryRepository).findFirstByArtistIdOrderByCreationDateDesc("artist2");
        verify(albumDiscoveryRepository).findFirstByAlbumIdOrderByCreationDateDesc("album1");
        verify(albumDiscoveryRepository).findFirstByAlbumIdOrderByCreationDateDesc("album2");
        verify(albumRepository, times(2)).findAll((Pageable) any());

        assertThat(progressCalls).hasSize(6);
        assertProgress(progressCalls.get(0), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 2));
        assertProgress(progressCalls.get(1), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(1, 2));
        assertProgress(progressCalls.get(2), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(2, 2));
        assertProgress(progressCalls.get(3), FULL_ALBUM_DISCOVERY, DiscoveryProgress.Value.of(0, 2));
        assertProgress(progressCalls.get(4), FULL_ALBUM_DISCOVERY, DiscoveryProgress.Value.of(1, 2));
        assertProgress(progressCalls.get(5), FULL_ALBUM_DISCOVERY, DiscoveryProgress.Value.of(2, 2));
    }

    @Test
    public void shouldReportEmptyLibraryProgress() {

        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of());
        when(albumRepository.findAll((Pageable) any())).thenAnswer(invocation ->
                new PageImpl<>(List.of(), invocation.getArgument(0), 0));

        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(discoveryJob(), progressCalls::add);

        assertThat(progressCalls).hasSize(2);
        assertProgress(progressCalls.get(0), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 0));
        assertProgress(progressCalls.get(1), FULL_ALBUM_DISCOVERY, DiscoveryProgress.Value.of(0, 0));
        verifyNoInteractions(artistDiscoveryRepository, albumDiscoveryRepository);
    }

    @Test
    public void shouldPreserveInterruptionDuringArtistDiscovery() {

        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist("artist1")));
        when(artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc("artist1"))
                .thenReturn(Optional.empty());

        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> fullDiscoveryService.discover(discoveryJob(), progressCalls::add))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }

        assertThat(progressCalls).hasSize(1);
        assertProgress(progressCalls.get(0), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 1));
        verifyNoInteractions(albumRepository, albumDiscoveryRepository);
    }

    @Test
    public void shouldPreserveInterruptionDuringAlbumDiscovery() {

        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of());
        when(albumRepository.findAll((Pageable) any())).thenAnswer(invocation ->
                new PageImpl<>(List.of(album("album1")), invocation.getArgument(0), 1));
        when(albumDiscoveryRepository.findFirstByAlbumIdOrderByCreationDateDesc("album1"))
                .thenReturn(Optional.empty());

        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> fullDiscoveryService.discover(discoveryJob(), progressCalls::add))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }

        assertThat(progressCalls).hasSize(2);
        assertProgress(progressCalls.get(0), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 0));
        assertProgress(progressCalls.get(1), FULL_ALBUM_DISCOVERY, DiscoveryProgress.Value.of(0, 1));
    }

    @Test
    public void shouldSkipUpToDateDiscoveries() {

        LocalDateTime now = LocalDateTime.now();
        Artist artist = artist("artist1").setUpdateDate(now.minusMinutes(1));
        Album album = album("album1").setUpdateDate(now.minusMinutes(1));

        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        when(albumRepository.findAll((Pageable) any())).thenAnswer(invocation ->
                new PageImpl<>(List.of(album), invocation.getArgument(0), 1));
        when(artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc("artist1"))
                .thenReturn(Optional.of(new ArtistDiscovery().setCreationDate(now)));
        when(albumDiscoveryRepository.findFirstByAlbumIdOrderByCreationDateDesc("album1"))
                .thenReturn(Optional.of(new AlbumDiscovery().setCreationDate(now)));

        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        Thread.currentThread().interrupt();
        try {
            fullDiscoveryService.discover(discoveryJob(), progressCalls::add);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }

        assertThat(progressCalls).hasSize(4);
        assertProgress(progressCalls.get(0), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 1));
        assertProgress(progressCalls.get(1), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(1, 1));
        assertProgress(progressCalls.get(2), FULL_ALBUM_DISCOVERY, DiscoveryProgress.Value.of(0, 1));
        assertProgress(progressCalls.get(3), FULL_ALBUM_DISCOVERY, DiscoveryProgress.Value.of(1, 1));
    }

    @Test
    public void shouldDiscoverOutdatedDiscoveries() {

        LocalDateTime now = LocalDateTime.now();
        Artist artist = artist("artist1").setUpdateDate(now);
        Album album = album("album1").setUpdateDate(now);

        when(artistRepository.findAll(any(Sort.class))).thenReturn(List.of(artist));
        when(albumRepository.findAll((Pageable) any())).thenAnswer(invocation ->
                new PageImpl<>(List.of(album), invocation.getArgument(0), 1));
        when(artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc("artist1"))
                .thenReturn(Optional.of(new ArtistDiscovery().setCreationDate(now.minusMinutes(1))));
        when(albumDiscoveryRepository.findFirstByAlbumIdOrderByCreationDateDesc("album1"))
                .thenReturn(Optional.of(new AlbumDiscovery().setCreationDate(now.minusMinutes(1))));

        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(discoveryJob(), progressCalls::add);

        assertThat(progressCalls).hasSize(4);
        assertProgress(progressCalls.get(0), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(0, 1));
        assertProgress(progressCalls.get(1), FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(1, 1));
        assertProgress(progressCalls.get(2), FULL_ALBUM_DISCOVERY, DiscoveryProgress.Value.of(0, 1));
        assertProgress(progressCalls.get(3), FULL_ALBUM_DISCOVERY, DiscoveryProgress.Value.of(1, 1));
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

    private Album album(String id) {
        return new Album()
                .setId(id)
                .setName("someAlbum")
                .setArtist(artist("artist1"));
    }
}
