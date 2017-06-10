package net.dorokhov.pony.library.service.impl;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.repository.ArtistRepository;
import net.dorokhov.pony.library.repository.SongRepository;
import net.dorokhov.pony.library.service.LibraryService;
import net.dorokhov.pony.library.service.exception.ArtistNotFoundException;
import net.dorokhov.pony.library.service.exception.SongNotFoundException;
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

    public LibraryServiceImpl(ArtistRepository artistRepository, SongRepository songRepository) {
        this.artistRepository = artistRepository;
        this.songRepository = songRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Artist> getArtists() {
        return ImmutableList.copyOf(artistRepository.findAll(new Sort("name")));
    }

    @Override
    @Transactional(readOnly = true)
    public Artist getArtistById(Long id) throws ArtistNotFoundException {
        Artist artist = artistRepository.findOne(id);
        if (artist == null) {
            throw new ArtistNotFoundException(id);
        }
        return artist;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Song> getSongsByIds(List<Long> ids) throws SongNotFoundException {
        Set<Long> notFoundIds = new HashSet<>(ids);
        ImmutableList.Builder<Song> listBuilder = ImmutableList.builder();
        for (Song song : songRepository.findAll(ids)) {
            listBuilder.add(song);
            notFoundIds.remove(song.getId());
        }
        if (notFoundIds.size() > 0) {
            Long songId = notFoundIds.iterator().next();
            throw new SongNotFoundException(songId);
        }
        return listBuilder.build();
    }
}
