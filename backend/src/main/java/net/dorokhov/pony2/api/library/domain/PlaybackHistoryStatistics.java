package net.dorokhov.pony2.api.library.domain;

public class PlaybackHistoryStatistics {

    private long totalCount;

    public long getTotalCount() {
        return totalCount;
    }

    public PlaybackHistoryStatistics setTotalCount(long totalCount) {
        this.totalCount = totalCount;
        return this;
    }
}
