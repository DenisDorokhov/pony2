package net.dorokhov.pony2.api.library.domain;

import jakarta.annotation.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Locale;

public record ArtistBiographyData(
        @Nullable String externalImageId,
        @Nullable List<ImageFace> faces,
        @Nullable String text,
        List<Link> links
) {
    public boolean isAvailable() {
        return (text != null && !text.isBlank()) || !links.isEmpty();
    }

    public record Link(LinkType type, String url) {
        public static Link of(SpotifyArtistData.ExternalLink link) {
            return new Link(LinkType.fromUrl(link.url()), link.url());
        }
    }

    public enum LinkType {
        INSTAGRAM("instagram.com"),
        FACEBOOK("facebook.com"),
        X("x.com", "twitter.com"),
        TIKTOK("tiktok.com"),
        WIKIPEDIA("wikipedia.org"),
        WHATSAPP("whatsapp.com"),
        WEBSITE;

        private final List<String> domains;

        LinkType(String... domains) {
            this.domains = List.of(domains);
        }

        public static LinkType fromUrl(String url) {
            String host;
            try {
                host = URI.create(url).getHost();
            } catch (IllegalArgumentException e) {
                return WEBSITE;
            }
            if (host == null) {
                return WEBSITE;
            }
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            for (LinkType type : values()) {
                if (type.domains.stream().anyMatch(domain -> normalizedHost.equals(domain) || normalizedHost.endsWith("." + domain))) {
                    return type;
                }
            }
            return WEBSITE;
        }
    }
}
