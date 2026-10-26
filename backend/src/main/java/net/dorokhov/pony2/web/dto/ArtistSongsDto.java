package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtistDiscoveryData;

import java.util.List;
import java.util.function.Function;

public final class ArtistSongsDto {

    private ArtistDetailsDto artist;
    private List<AlbumSongsDto> albumSongs;

    public ArtistDetailsDto getArtist() {
        return artist;
    }

    public ArtistSongsDto setArtist(ArtistDetailsDto artist) {
        this.artist = artist;
        return this;
    }

    public List<AlbumSongsDto> getAlbumSongs() {
        return albumSongs;
    }

    public ArtistSongsDto setAlbumSongs(List<AlbumSongsDto> albumSongs) {
        this.albumSongs = albumSongs;
        return this;
    }

    public static ArtistSongsDto of(Artist artist, @Nullable ArtistDiscoveryData artistDiscoveryData, Function<Album, AlbumSongsDto> albumMapper) {
        return new ArtistSongsDto()
                .setArtist(ArtistDetailsDto.of(artist, artistDiscoveryData))
                .setAlbumSongs(artist.getAlbums().stream()
                        .sorted()
                        .map(albumMapper)
                        .toList());
    }
}
