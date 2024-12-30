package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.LibraryService;
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
import java.util.stream.Collectors;

@Service
public class LibraryServiceImpl implements LibraryService {

    private final static int PAGE_SIZE = 50;

    private final GenreRepository genreRepository;
    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;
    private final ArtworkStorage artworkStorage;
    private final RandomFetcher randomFetcher;

    public LibraryServiceImpl(
            GenreRepository genreRepository,
            ArtistRepository artistRepository,
            SongRepository songRepository,
            ArtworkStorage artworkStorage,
            RandomFetcher randomFetcher
    ) {
        this.genreRepository = genreRepository;
        this.artistRepository = artistRepository;
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
            return Collections.emptyList();
        }
        Set<String> excludeArtistIds = new HashSet<>();
        if (request.getLastArtistId() != null) {
            excludeArtistIds.add(request.getLastArtistId());
        }
        if (!request.getGenreIds().isEmpty()) {
            return randomFetcher.fetch(request.getCount(), new RandomFetcher.Repository<>() {

                @Override
                public long fetchCount() {
                    long result = excludeArtistIds.isEmpty() ? songRepository.countByGenreIdIn(request.getGenreIds()) :
                            songRepository.countByGenreIdInAndArtistIdNotIn(request.getGenreIds(), excludeArtistIds);
                    if (result == 0 && !excludeArtistIds.isEmpty()) {
                        excludeArtistIds.clear();
                        result = songRepository.countByGenreIdIn(request.getGenreIds());
                    }
                    return result;
                }

                @Override
                public List<Song> fetchContent(Pageable pageable) {
                    List<Song> result = excludeArtistIds.isEmpty() ? songRepository.findByGenreIdIn(request.getGenreIds(), pageable) :
                            songRepository.findByGenreIdInAndArtistIdNotIn(request.getGenreIds(), excludeArtistIds, pageable);
                    if (result.isEmpty() && !excludeArtistIds.isEmpty()) {
                        excludeArtistIds.clear();
                        result = songRepository.findByGenreIdIn(request.getGenreIds(), pageable);
                    } else {
                        excludeArtistIds.addAll(result.stream()
                                .map(Song::getArtist)
                                .map(Artist::getId)
                                .collect(Collectors.toSet()));
                    }
                    return result;
                }
            });
        } else {
            return randomFetcher.fetch(request.getCount(), new RandomFetcher.Repository<>() {

                @Override
                public long fetchCount() {
                    long result = excludeArtistIds.isEmpty() ? songRepository.count() :
                            songRepository.countByArtistIdNotIn(excludeArtistIds);
                    if (result == 0 && !excludeArtistIds.isEmpty()) {
                        excludeArtistIds.clear();
                        result = songRepository.count();
                    }
                    return result;
                }

                @Override
                public List<Song> fetchContent(Pageable pageable) {
                    List<Song> result = excludeArtistIds.isEmpty() ? songRepository.findAllBy(pageable) :
                            songRepository.findByArtistIdNotIn(excludeArtistIds, pageable);
                    if (result.isEmpty() && !excludeArtistIds.isEmpty()) {
                        excludeArtistIds.clear();
                        result = songRepository.findAllBy(pageable);
                    } else {
                        excludeArtistIds.addAll(result.stream()
                                .map(Song::getAlbum)
                                .map(Album::getArtist)
                                .map(Artist::getId)
                                .collect(Collectors.toSet()));
                    }
                    return result;
                }
            });
        }
    }
}
