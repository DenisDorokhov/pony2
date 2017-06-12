package net.dorokhov.pony.library.service.impl;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.Artwork;
import net.dorokhov.pony.library.domain.ArtworkFiles;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.repository.ArtistRepository;
import net.dorokhov.pony.library.repository.SongRepository;
import net.dorokhov.pony.library.service.LibraryService;
import net.dorokhov.pony.library.service.exception.ObjectNotFoundException;
import net.dorokhov.pony.library.service.impl.artwork.ArtworkStorage;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LibraryServiceImpl implements LibraryService {
    
    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;
    private final ArtworkStorage artworkStorage;

    public LibraryServiceImpl(ArtistRepository artistRepository, 
                              SongRepository songRepository, 
                              ArtworkStorage artworkStorage) {
        this.artistRepository = artistRepository;
        this.songRepository = songRepository;
        this.artworkStorage = artworkStorage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Artist> getArtists() {
        return ImmutableList.copyOf(artistRepository.findAll(new Sort("name")));
    }

    @Override
    @Transactional(readOnly = true)
    public Artist getArtistById(Long id) throws ObjectNotFoundException {
        Artist artist = artistRepository.findOne(id);
        if (artist == null) {
            throw new ObjectNotFoundException(id, Artist.class);
        }
        return artist;
    }

    @Override
    @Transactional(readOnly = true)
    public Song getSongById(Long id) throws ObjectNotFoundException {
        Song song = songRepository.findOne(id);
        if (song == null) {
            throw new ObjectNotFoundException(id, Song.class);
        }
        return song;
    }

    @Override
    @Transactional(readOnly = true)
    public ArtworkFiles getArtworkFilesById(Long id) throws ObjectNotFoundException {
        ArtworkFiles artworkFiles = artworkStorage.getArtworkFile(id);
        if (artworkFiles == null) {
            throw new ObjectNotFoundException(id, Artwork.class);
        }
        return artworkFiles;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Song> getSongsByIds(List<Long> ids) throws ObjectNotFoundException {
        Set<Long> notFoundIds = new HashSet<>(ids);
        ImmutableList.Builder<Song> listBuilder = ImmutableList.builder();
        for (Song song : songRepository.findAll(ids)) {
            listBuilder.add(song);
            notFoundIds.remove(song.getId());
        }
        if (notFoundIds.size() > 0) {
            Long songId = notFoundIds.iterator().next();
            throw new ObjectNotFoundException(songId, Song.class);
        }
        return listBuilder.build();
    }
}
