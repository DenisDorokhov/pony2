package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.service.PlaybackHistoryService;

import java.util.List;

public class RestoredHistoryDto {

    private int restoredSongCount;
    private int notFoundSongCount;
    private List<String> notFoundUserEmails;

    public int getRestoredSongCount() {
        return restoredSongCount;
    }

    public RestoredHistoryDto setRestoredSongCount(int restoredSongCount) {
        this.restoredSongCount = restoredSongCount;
        return this;
    }

    public int getNotFoundSongCount() {
        return notFoundSongCount;
    }

    public RestoredHistoryDto setNotFoundSongCount(int notFoundSongCount) {
        this.notFoundSongCount = notFoundSongCount;
        return this;
    }

    public List<String> getNotFoundUserEmails() {
        return notFoundUserEmails;
    }

    public RestoredHistoryDto setNotFoundUserEmails(List<String> notFoundUserEmails) {
        this.notFoundUserEmails = notFoundUserEmails;
        return this;
    }

    public static RestoredHistoryDto of(PlaybackHistoryService.RestoredHistory restoredHistory) {
        return new RestoredHistoryDto()
                .setRestoredSongCount(restoredHistory.restoredSongCount())
                .setNotFoundSongCount(restoredHistory.notFoundSongCount())
                .setNotFoundUserEmails(restoredHistory.notFoundUserEmails());
    }
}
