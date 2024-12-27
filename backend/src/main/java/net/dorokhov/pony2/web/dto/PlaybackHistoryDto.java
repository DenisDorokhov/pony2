package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.PlaybackHistorySong;
import net.dorokhov.pony2.api.library.domain.PlaybackHistoryStatistics;

import java.util.List;

public class PlaybackHistoryDto {

    public static class Statistics {

        private long totalCount;

        public long getTotalCount() {
            return totalCount;
        }

        public Statistics setTotalCount(long totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        public static Statistics of(PlaybackHistoryStatistics statistics) {
            return new Statistics()
                    .setTotalCount(statistics.getTotalCount());
        }
    }

    private List<PlaybackHistorySongDto> songs;
    private Statistics statistics;

    public List<PlaybackHistorySongDto> getSongs() {
        return songs;
    }

    public PlaybackHistoryDto setSongs(List<PlaybackHistorySongDto> songs) {
        this.songs = songs;
        return this;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public PlaybackHistoryDto setStatistics(Statistics statistics) {
        this.statistics = statistics;
        return this;
    }

    public static PlaybackHistoryDto of(List<PlaybackHistorySong> songs, PlaybackHistoryStatistics statistics, boolean isAdmin) {
        return new PlaybackHistoryDto()
                .setSongs(songs.stream()
                        .map(song -> PlaybackHistorySongDto.of(song, isAdmin))
                        .toList())
                .setStatistics(Statistics.of(statistics));
    }
}
