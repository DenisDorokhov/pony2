package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicBookmark;

import java.util.List;

public class OpenSubsonicBookmarksResponse extends OpenSubsonicResponse.AbstractResponse<OpenSubsonicBookmarksResponse> {

    private Bookmarks bookmarks;

    public Bookmarks getBookmarks() {
        return bookmarks;
    }

    public OpenSubsonicBookmarksResponse setBookmarks(Bookmarks bookmarks) {
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
