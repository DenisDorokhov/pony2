package net.dorokhov.pony2.api.library.service;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LibraryService {

    List<Genre> getGenres();

    List<Artist> getArtists();

    List<Artist> getArtists(int size, int offset);

    List<Song> getSongsByIds(List<String> ids);

    Optional<Artist> getArtistById(String artistId);

    List<Artist> getArtistsBySpotifyIds(List<String> spotifyArtistIds);

    Optional<ArtistDiscoveryData> getArtistDiscoveryData(String artistId);

    void updateArtistDiscovery(Artist artist, @Nullable CountryCode overrideCountryCode);

    Map<String, ArtistDiscoveryData> getArtistDiscoveryData(List<String> artistIds);

    Optional<ArtistBiographyData> getArtistBiographyData(String artistId);

    Optional<AlbumDiscoveryData> getAlbumDiscoveryData(String albumId);

    void updateAlbumDiscovery(Album album, @Nullable AlbumDiscoveryData.ReleaseType overrideReleaseType);

    List<Album> getAlbums(int size, int offset);

    Optional<Album> getAlbumById(String id);

    Optional<Song> getSongById(String id);

    List<Song> getSongs(int size, int offset);

    Optional<ArtworkFiles> getArtworkFilesById(String id);

    List<Song> getRandomSongs(RandomSongsRequest request);
}
