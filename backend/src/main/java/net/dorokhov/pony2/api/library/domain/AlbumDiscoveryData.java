package net.dorokhov.pony2.api.library.domain;

public record AlbumDiscoveryData(ReleaseType releaseType) {
    public enum ReleaseType {
        LP, EP, SINGLE, LIVE, COMPILATION, UNKNOWN
    }
}
