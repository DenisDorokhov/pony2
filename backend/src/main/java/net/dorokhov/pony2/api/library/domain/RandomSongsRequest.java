package net.dorokhov.pony2.api.library.domain;

import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RandomSongsRequest {

    private int count;
    private String lastArtistId;
    private List<String> genreIds;

    public int getCount() {
        return count;
    }

    public RandomSongsRequest setCount(int count) {
        this.count = count;
        return this;
    }

    @Nullable
    public String getLastArtistId() {
        return lastArtistId;
    }

    public RandomSongsRequest setLastArtistId(String lastArtistId) {
        this.lastArtistId = lastArtistId;
        return this;
    }

    public List<String> getGenreIds() {
        if (genreIds == null) {
            genreIds = new ArrayList<>();
        }
        return genreIds;
    }

    public RandomSongsRequest setGenreIds(List<String> genreIds) {
        this.genreIds = genreIds;
        return this;
    }
}
