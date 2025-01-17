package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.LibraryService;
import net.dorokhov.pony2.core.library.repository.ArtistGenreRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.GenreRepository;
import net.dorokhov.pony2.core.library.repository.SongRepository;
import net.dorokhov.pony2.core.library.service.artwork.ArtworkStorage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Service
public class LibraryServiceImpl implements LibraryService {

    private final static int PAGE_SIZE = 50;

    private final GenreRepository genreRepository;
    private final ArtistRepository artistRepository;
    private final ArtistGenreRepository artistGenreRepository;
    private final SongRepository songRepository;
    private final ArtworkStorage artworkStorage;
    private final RandomFetcher randomFetcher;

    public LibraryServiceImpl(
            GenreRepository genreRepository,
            ArtistRepository artistRepository,
            ArtistGenreRepository artistGenreRepository,
            SongRepository songRepository,
            ArtworkStorage artworkStorage,
            RandomFetcher randomFetcher
    ) {
        this.genreRepository = genreRepository;
        this.artistRepository = artistRepository;
        this.artistGenreRepository = artistGenreRepository;
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
    public Page<Song> getSongsByGenreId(String genreId, int pageIndex) {
        return songRepository.findPageByGenreId(genreId, PageRequest.of(pageIndex, PAGE_SIZE,
                Sort.by("album.year", "album.name", "discNumber", "trackNumber", "name")));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Genre> getGenreById(String genreId) {
        return genreRepository.findById(genreId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Artist> getArtistById(String id) {
        return artistRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Song> getSongById(String id) {
        return songRepository.findById(id);
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
