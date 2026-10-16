package net.dorokhov.pony2.api.library.domain;

public enum DiscoveryTaskType {
    SPOTIFY_ARTIST_DATA(true),
    SPOTIFY_TOP_TRACKS(true),
    MUSICBRAINZ_ALBUM_DATA(false);

    private final boolean toolsEnabled;

    DiscoveryTaskType(boolean toolsEnabled) {
        this.toolsEnabled = toolsEnabled;
    }

    public boolean isToolsEnabled() {
        return toolsEnabled;
    }
}
