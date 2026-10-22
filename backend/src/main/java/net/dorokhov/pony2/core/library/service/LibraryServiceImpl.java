package net.dorokhov.pony2.core.library.service;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData.ReleaseType;
import net.dorokhov.pony2.api.library.service.LibraryService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.common.OffsetBasedPageRequest;
import net.dorokhov.pony2.core.library.repository.*;
import net.dorokhov.pony2.core.library.service.artwork.ArtworkStorage;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Service
public class LibraryServiceImpl implements LibraryService {

    private final GenreRepository genreRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final ArtistGenreRepository artistGenreRepository;
    private final ArtistDiscoveryIndexRepository artistDiscoveryIndexRepository;
    private final DiscoveryTaskRepository discoveryTaskRepository;
    private final SongRepository songRepository;
    private final ArtworkStorage artworkStorage;
    private final RandomFetcher randomFetcher;

    public LibraryServiceImpl(
            GenreRepository genreRepository,
            ArtistRepository artistRepository,
            AlbumRepository albumRepository,
            ArtistGenreRepository artistGenreRepository,
            ArtistDiscoveryIndexRepository artistDiscoveryIndexRepository,
            DiscoveryTaskRepository discoveryTaskRepository,
            SongRepository songRepository,
            ArtworkStorage artworkStorage,
            RandomFetcher randomFetcher
    ) {
        this.genreRepository = genreRepository;
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.artistGenreRepository = artistGenreRepository;
        this.artistDiscoveryIndexRepository = artistDiscoveryIndexRepository;
        this.discoveryTaskRepository = discoveryTaskRepository;
        this.songRepository = songRepository;
        this.artworkStorage = artworkStorage;
        this.randomFetcher = randomFetcher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Genre> getGenres() {
        return genreRepository.findAll(Sort.by("name"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Artist> getArtists() {
        return artistRepository.findAll(Sort.by("name"));
    }

    @Override
    public List<Artist> getArtists(int size, int offset) {
        return artistRepository.findAll(new OffsetBasedPageRequest(size, offset, Sort.by("id"))).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Song> getSongsByIds(List<String> ids) {
        Map<String, Song> songs = songRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Song::getId, song -> song));
        // Preserve order.
        return ids.stream()
                .map(songs::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Artist> getArtistById(String id) {
        return artistRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ArtistDiscoveryData getArtistDiscoveryData(String artistId) {
        List<String> topSongIds = discoveryTaskRepository.findLatestCompletedSpotifyTopTracksResultByArtistId(artistId, Limit.of(1))
                .map(json -> JsonConverter.listFromJson(json, String.class)).orElse(emptyList()).stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!topSongIds.isEmpty()) {
            Set<String> existingSongIds = new HashSet<>(songRepository.findExistingIds(topSongIds));
            topSongIds = topSongIds.stream()
                    .filter(existingSongIds::contains)
                    .toList();
        }
        return new ArtistDiscoveryData(artistDiscoveryIndexRepository.findByArtistId(artistId)
                .map(ArtistDiscoveryIndex::getCountryCode)
                .orElse(null), topSongIds);
    }

    @Override
    @Transactional(readOnly = true)
    public AlbumDiscoveryData getAlbumDiscoveryData(String albumId) {
        MusicBrainzAlbumData data = discoveryTaskRepository.findLatestCompletedMusicBrainzResultByAlbumId(albumId, Limit.of(1))
                .map(json -> JsonConverter.fromJson(json, MusicBrainzAlbumData.class)).orElse(null);
        if (data == null) {
            return new AlbumDiscoveryData(ReleaseType.UNKNOWN, null);
        }
        ReleaseType releaseType;
        if (data.secondaryTypes().contains("Compilation")) {
            releaseType = ReleaseType.COMPILATION;
        } else if (data.secondaryTypes().contains("Live")) {
            releaseType = ReleaseType.LIVE;
        } else {
            releaseType = switch (data.primaryType()) {
                case "Album" -> ReleaseType.LP;
                case "EP" -> ReleaseType.EP;
                case "Single" -> ReleaseType.SINGLE;
                case null, default -> ReleaseType.UNKNOWN;
            };
        }
        return new AlbumDiscoveryData(releaseType, parseReleaseDate(data.firstReleaseDate()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Album> getAlbums(int size, int offset) {
        return albumRepository.findAll(new OffsetBasedPageRequest(size, offset, Sort.by("id"))).getContent();
    }

    @Override
    public Optional<Album> getAlbumById(String id) {
        return albumRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Song> getSongById(String id) {
        return songRepository.findById(id);
    }

    @Override
    public List<Song> getSongs(int size, int offset) {
        return songRepository.findAll(new OffsetBasedPageRequest(size, offset, Sort.by("id"))).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ArtworkFiles> getArtworkFilesById(String id) {
        return artworkStorage.getArtworkFile(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Song> getRandomSongs(RandomSongsRequest request) {
        if (request.getCount() == 0) {
            return emptyList();
        }
        if (!request.getGenreIds().isEmpty()) {
            List<String> allArtistIds = artistGenreRepository.findAllArtistIdsByGenreIdIn(request.getGenreIds());
            if (allArtistIds.isEmpty()) {
                return emptyList();
            }
            return randomFetcher.fetch(request.getCount(), new RandomFetcherRepository(allArtistIds, request.getLastArtistId()));
        } else {
            List<String> allArtistIds = artistRepository.findAllIds();
            if (allArtistIds.isEmpty()) {
                return emptyList();
            }
            return randomFetcher.fetch(request.getCount(), new RandomFetcherRepository(allArtistIds, request.getLastArtistId()));
        }
    }

    @Nullable
    private LocalDateTime parseReleaseDate(@Nullable String releaseDate) {
        if (releaseDate == null || releaseDate.isBlank()) {
            return null;
        }
        try {
            String fullDate = switch (releaseDate.length()) {
                case 4 -> releaseDate + "-01-01";
                case 7 -> releaseDate + "-01";
                default -> releaseDate;
            };
            return LocalDate.parse(fullDate).atStartOfDay();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private class RandomFetcherRepository implements RandomFetcher.Repository<Song> {

        private final List<String> allArtistIds;
        private final List<String> artistPool;

        private final AtomicReference<String> artistId = new AtomicReference<>();

        private RandomFetcherRepository(List<String> allArtistIds, String lastArtistId) {
            this.allArtistIds = allArtistIds;
            artistPool = new ArrayList<>(allArtistIds.stream()
                    .filter(next -> !next.equals(lastArtistId))
                    .toList());
            if (artistPool.isEmpty()) {
                artistPool.addAll(allArtistIds);
            }
            Collections.shuffle(artistPool);
        }

        @Override
        public long fetchCount() {
            artistId.set(artistPool.removeFirst());
            if (artistPool.isEmpty()) {
                allArtistIds.stream()
                        .filter(next -> !next.equals(artistId.get()))
                        .forEach(artistPool::add);
                if (artistPool.isEmpty()) {
                    artistPool.addAll(allArtistIds);
                }
                Collections.shuffle(artistPool);
            }
            return songRepository.countByArtistId(artistId.get());
        }

        @Override
        public List<Song> fetchContent(Pageable pageable) {
            return songRepository.findByArtistId(artistId.get(), pageable);
        }
    }
}
