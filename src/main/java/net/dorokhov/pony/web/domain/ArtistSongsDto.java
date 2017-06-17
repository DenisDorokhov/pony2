package net.dorokhov.pony.web.domain;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.library.domain.Artist;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ArtistSongsDto {

    private final ArtistDto artist;
    private final List<AlbumSongsDto> albums;

    public ArtistSongsDto(ArtistDto artist, List<AlbumSongsDto> albums) {
        this.artist = checkNotNull(artist);
        this.albums = ImmutableList.copyOf(albums);
    }

    public ArtistDto getArtist() {
        return artist;
    }

    public List<AlbumSongsDto> getAlbums() {
        return albums;
    }

    public static ArtistSongsDto of(Artist artist) {
        return new ArtistSongsDto(ArtistDto.of(artist), artist.getAlbums().stream()
                .sorted()
                .map(AlbumSongsDto::of)
                .collect(Collectors.toList()));
    }
}
