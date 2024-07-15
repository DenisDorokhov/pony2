package net.dorokhov.pony3.core.library.service;

import net.dorokhov.pony3.api.library.domain.Artist;
import net.dorokhov.pony3.api.library.domain.ArtworkFiles;
import net.dorokhov.pony3.api.library.domain.Genre;
import net.dorokhov.pony3.api.library.domain.Song;
import net.dorokhov.pony3.api.library.service.LibraryService;
import net.dorokhov.pony3.core.library.repository.ArtistRepository;
import net.dorokhov.pony3.core.library.repository.GenreRepository;
import net.dorokhov.pony3.core.library.repository.SongRepository;
import net.dorokhov.pony3.core.library.service.artwork.ArtworkStorage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
        return songRepository.findByGenreId(genreId, PageRequest.of(pageIndex, PAGE_SIZE,
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
    public List<Song> getRandomSongs(int count) {
        return randomFetcher.fetch(count, new RandomFetcher.Repository<>() {

            @Override
            public long fetchCount() {
                return songRepository.count();
            }

            @Override
            public List<Song> fetchContent(Pageable pageable) {
                return songRepository.findAll(pageable).getContent();
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Song> getRandomSongsByAlbumId(String albumId, int count) {
        return randomFetcher.fetch(count, new RandomFetcher.Repository<>() {

            @Override
            public long fetchCount() {
                return songRepository.countByAlbumId(albumId);
            }

            @Override
            public List<Song> fetchContent(Pageable pageable) {
                return songRepository.findByAlbumId(albumId, pageable);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Song> getRandomSongsByArtistId(String artistId, int count) {
        return randomFetcher.fetch(count, new RandomFetcher.Repository<>() {

            @Override
            public long fetchCount() {
                return songRepository.countByAlbumArtistId(artistId);
            }

            @Override
            public List<Song> fetchContent(Pageable pageable) {
                return songRepository.findByAlbumArtistId(artistId, pageable);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Song> getRandomSongsByGenreId(String genreId, int count) {
        return randomFetcher.fetch(count, new RandomFetcher.Repository<>() {

            @Override
            public long fetchCount() {
                return songRepository.countByGenreId(genreId);
            }

            @Override
            public List<Song> fetchContent(Pageable pageable) {
                return songRepository.findByGenreId(genreId, pageable).getContent();
            }
        });
    }
}
