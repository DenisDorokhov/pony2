package net.dorokhov.pony2.test;

import net.dorokhov.pony2.api.library.domain.SpotifyArtistData;
import net.dorokhov.pony2.common.JsonConverter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SpotifyArtistDataFixtures {

    private SpotifyArtistDataFixtures() {}

    public static String spotifyArtistResponse(SpotifyArtistData data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "FOUND");
        response.putAll(JsonConverter.mapFromJson(JsonConverter.toJson(data), String.class, Object.class));
        return JsonConverter.toJson(response);
    }

    public static String spotifyArtistResponse(String status) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status);
        for (String field : List.of("artist", "matchedAlbum", "topTracks", "similarArtists", "biography", "links", "imageUrl", "avatarUrl")) {
            response.put(field, null);
        }
        return JsonConverter.toJson(response);
    }
}
