package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.library.domain.Artist;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableList;

public final class ArtistSongsDto {

    private final ArtistDto artist;
    private final List<AlbumSongsDto> albumSongs;

    private ArtistSongsDto(ArtistDto artist, List<AlbumSongsDto> albumSongs) {
        this.artist = checkNotNull(artist);
        this.albumSongs = unmodifiableList(albumSongs);
    }

    public ArtistDto getArtist() {
        return artist;
    }

    public List<AlbumSongsDto> getAlbumSongs() {
        return albumSongs;
    }

    public static ArtistSongsDto of(Artist artist) {
        return new ArtistSongsDto(ArtistDto.of(artist), artist.getAlbums().stream()
                .sorted()
                .map(AlbumSongsDto::of)
                .collect(Collectors.toList()));
    }
}
