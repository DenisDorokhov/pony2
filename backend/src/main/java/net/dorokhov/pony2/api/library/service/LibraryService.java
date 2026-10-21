package net.dorokhov.pony2.api.library.service;

import net.dorokhov.pony2.api.library.domain.*;

import java.util.List;
import java.util.Optional;

public interface LibraryService {

    List<Genre> getGenres();

    List<Artist> getArtists();

    List<Artist> getArtists(int size, int offset);

    List<Song> getSongsByIds(List<String> ids);

    Optional<Artist> getArtistById(String artistId);

    ArtistDiscoveryData getArtistDiscoveryData(String artistId);

    AlbumDiscoveryData getAlbumDiscoveryData(String albumId);

    List<Album> getAlbums(int size, int offset);

    Optional<Album> getAlbumById(String id);

    Optional<Song> getSongById(String id);

    List<Song> getSongs(int size, int offset);

    Optional<ArtworkFiles> getArtworkFilesById(String id);

    List<Song> getRandomSongs(RandomSongsRequest request);
}
