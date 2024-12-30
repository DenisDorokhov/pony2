package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import net.dorokhov.pony2.api.library.domain.RandomSongsRequest;

import java.util.List;

public class RandomSongsRequestDto {

    private int count;
    private String lastArtistId;
    private List<String> genreIds;

    @Min(1)
    @Max(50)
    public int getCount() {
        return count;
    }

    public RandomSongsRequestDto setCount(int count) {
        this.count = count;
        return this;
    }

    @Nullable
    public String getLastArtistId() {
        return lastArtistId;
    }

    public RandomSongsRequestDto setLastArtistId(String lastArtistId) {
        this.lastArtistId = lastArtistId;
        return this;
    }

    public List<String> getGenreIds() {
        return genreIds;
    }

    public RandomSongsRequestDto setGenreIds(List<String> genreIds) {
        this.genreIds = genreIds;
        return this;
    }

    public RandomSongsRequest convert() {
        return new RandomSongsRequest()
                .setCount(count)
                .setLastArtistId(lastArtistId)
                .setGenreIds(genreIds);
    }
}
