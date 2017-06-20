package net.dorokhov.pony.library.service.impl;

import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.ArtworkFiles;
import net.dorokhov.pony.library.domain.Genre;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.repository.ArtistRepository;
import net.dorokhov.pony.library.repository.GenreRepository;
import net.dorokhov.pony.library.repository.SongRepository;
import net.dorokhov.pony.library.service.LibraryService;
import net.dorokhov.pony.library.service.impl.artwork.ArtworkStorage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.List;

import static java.util.Collections.unmodifiableList;

@Service
public class LibraryServiceImpl implements LibraryService {
    
    private final static int PAGE_SIZE = 50;
    
    private final GenreRepository genreRepository;
    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;
    private final ArtworkStorage artworkStorage;

    public LibraryServiceImpl(GenreRepository genreRepository, 
                              ArtistRepository artistRepository,
                              SongRepository songRepository,
                              ArtworkStorage artworkStorage) {
        this.genreRepository = genreRepository;
        this.artistRepository = artistRepository;
        this.songRepository = songRepository;
        this.artworkStorage = artworkStorage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Genre> getGenres() {
        return unmodifiableList(genreRepository.findAll(new Sort("name")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Artist> getArtists() {
        return unmodifiableList(artistRepository.findAll(new Sort("name")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Song> getSongsByIds(List<Long> ids) {
        return unmodifiableList(songRepository.findAll(ids));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Song> getSongsByGenreId(Long genreId, int pageIndex) {
        return songRepository.findByGenreId(genreId, new PageRequest(pageIndex, PAGE_SIZE, 
                new Sort("album.year", "album.name", "discNumber", "trackNumber", "name")));
    }

    @Override
    @Transactional(readOnly = true)
    @Nullable
    public Genre getGenreById(Long genreId) {
        return genreRepository.findOne(genreId);
    }

    @Override
    @Transactional(readOnly = true)
    @Nullable
    public Artist getArtistById(Long id) {
        return artistRepository.findOne(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Nullable
    public Song getSongById(Long id) {
        return songRepository.findOne(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Nullable
    public ArtworkFiles getArtworkFilesById(Long id) {
        return artworkStorage.getArtworkFile(id);
    }
}
