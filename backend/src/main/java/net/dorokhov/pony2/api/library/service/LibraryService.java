package net.dorokhov.pony2.api.library.service;

import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtworkFiles;
import net.dorokhov.pony2.api.library.domain.Genre;
import net.dorokhov.pony2.api.library.domain.Song;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface LibraryService {

    List<Genre> getGenres();

    List<Artist> getArtists();

    List<Song> getSongsByIds(List<String> ids);

    Page<Song> getSongsByGenreId(String genreId, int pageIndex);

    Optional<Genre> getGenreById(String genreId);

    Optional<Artist> getArtistById(String artistId);

    Optional<Song> getSongById(String id);

    Optional<ArtworkFiles> getArtworkFilesById(String id);

    List<Song> getRandomSongs(int count);

    List<Song> getRandomSongsByAlbumId(String albumId, int count);

    List<Song> getRandomSongsByArtistId(String artistId, int count);

    List<Song> getRandomSongsByGenreId(String genreId, int count);
}
