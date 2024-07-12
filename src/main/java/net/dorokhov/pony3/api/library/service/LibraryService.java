package net.dorokhov.pony3.api.library.service;

import jakarta.annotation.Nullable;
import net.dorokhov.pony3.api.library.domain.Artist;
import net.dorokhov.pony3.api.library.domain.ArtworkFiles;
import net.dorokhov.pony3.api.library.domain.Genre;
import net.dorokhov.pony3.api.library.domain.Song;
import org.springframework.data.domain.Page;

import java.util.List;

public interface LibraryService {

    List<Genre> getGenres();

    List<Artist> getArtists();

    List<Song> getSongsByIds(List<String> ids);

    Page<Song> getSongsByGenreId(String genreId, int pageIndex);

    @Nullable
    Genre getGenreById(String genreId);

    @Nullable
    Artist getArtistById(String artistId);

    @Nullable
    Song getSongById(String id);

    @Nullable
    ArtworkFiles getArtworkFilesById(String id);

    List<Song> getRandomSongs(int count);

    List<Song> getRandomSongsByAlbumId(String albumId, int count);

    List<Song> getRandomSongsByArtistId(String artistId, int count);

    List<Song> getRandomSongsByGenreId(String genreId, int count);
}
