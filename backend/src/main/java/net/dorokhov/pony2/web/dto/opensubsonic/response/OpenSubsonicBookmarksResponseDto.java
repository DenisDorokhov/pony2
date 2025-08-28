package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicBookmark;

import java.util.List;

public class OpenSubsonicBookmarksResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicBookmarksResponseDto> {

    private Bookmarks bookmarks;

    public Bookmarks getBookmarks() {
        return bookmarks;
    }

    public OpenSubsonicBookmarksResponseDto setBookmarks(Bookmarks bookmarks) {
        this.bookmarks = bookmarks;
        return this;
    }

    public static class Bookmarks {

        private List<OpenSubsonicBookmark> bookmark;

        public List<OpenSubsonicBookmark> getBookmark() {
            return bookmark;
        }

        public Bookmarks setBookmark(List<OpenSubsonicBookmark> bookmark) {
            this.bookmark = bookmark;
            return this;
        }
    }
}
