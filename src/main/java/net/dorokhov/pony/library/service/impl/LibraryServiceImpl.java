package net.dorokhov.pony.library.service.impl;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.ArtworkFiles;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.repository.ArtistRepository;
import net.dorokhov.pony.library.repository.SongRepository;
import net.dorokhov.pony.library.service.LibraryService;
import net.dorokhov.pony.library.service.impl.artwork.ArtworkStorage;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.List;

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
    public List<Song> getSongsByIds(List<Long> ids) {
        return ImmutableList.copyOf(songRepository.findAll(ids));
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
