package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.ArtistBiographyData;

import java.util.List;

public final class ArtistBiographyDto {

    private ArtistDetailsDto artist;
    private String externalImageId;
    private List<ImageFaceDto> faces;
    private String text;
    private List<Link> links;

    public ArtistDetailsDto getArtist() {
        return artist;
    }

    public ArtistBiographyDto setArtist(ArtistDetailsDto artist) {
        this.artist = artist;
        return this;
    }

    public @Nullable String getExternalImageId() {
        return externalImageId;
    }

    public ArtistBiographyDto setExternalImageId(@Nullable String externalImageId) {
        this.externalImageId = externalImageId;
        return this;
    }

    public @Nullable List<ImageFaceDto> getFaces() {
        return faces;
    }

    public ArtistBiographyDto setFaces(@Nullable List<ImageFaceDto> faces) {
        this.faces = faces;
        return this;
    }

    public @Nullable String getText() {
        return text;
    }

    public ArtistBiographyDto setText(@Nullable String text) {
        this.text = text;
        return this;
    }

    public List<Link> getLinks() {
        return links;
    }

    public ArtistBiographyDto setLinks(List<Link> links) {
        this.links = links;
        return this;
    }

    public static ArtistBiographyDto of(ArtistDetailsDto artist, @Nullable ArtistBiographyData biography) {
        return new ArtistBiographyDto()
                .setArtist(artist)
                .setExternalImageId(biography != null ? biography.externalImageId() : null)
                .setFaces(biography != null && biography.faces() != null
                        ? biography.faces().stream().map(ImageFaceDto::of).toList() : null)
                .setText(biography != null ? biography.text() : null)
                .setLinks(biography != null
                        ? biography.links().stream().map(link -> new Link(link.type(), link.url())).toList() : List.of());
    }

    public record Link(ArtistBiographyData.LinkType type, String url) {}
}
