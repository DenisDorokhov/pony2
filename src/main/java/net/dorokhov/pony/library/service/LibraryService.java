package net.dorokhov.pony.library.service;

import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.ArtworkFiles;
import net.dorokhov.pony.library.domain.Song;

import javax.annotation.Nullable;
import java.util.List;

public interface LibraryService {

    List<Artist> getArtists();

    List<Song> getSongsByIds(List<Long> ids);

    @Nullable
    Artist getArtistById(Long artistId);

    @Nullable
    Song getSongById(Long id);

    @Nullable
    ArtworkFiles getArtworkFilesById(Long id);
}
