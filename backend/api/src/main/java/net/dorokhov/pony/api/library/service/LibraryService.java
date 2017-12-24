package net.dorokhov.pony.api.library.service;

import net.dorokhov.pony.api.library.domain.Artist;
import net.dorokhov.pony.api.library.domain.ArtworkFiles;
import net.dorokhov.pony.api.library.domain.Genre;
import net.dorokhov.pony.api.library.domain.Song;
import org.springframework.data.domain.Page;

import javax.annotation.Nullable;
import java.util.List;

public interface LibraryService {
    
    List<Genre> getGenres();

    List<Artist> getArtists();

    List<Song> getSongsByIds(List<Long> ids);
    
    Page<Song> getSongsByGenreId(Long genreId, int pageIndex);
    
    @Nullable
    Genre getGenreById(Long genreId);

    @Nullable
    Artist getArtistById(Long artistId);

    @Nullable
    Song getSongById(Long id);

    @Nullable
    ArtworkFiles getArtworkFilesById(Long id);
}
