package net.dorokhov.pony2.web.dto.opensubsonic;

import java.util.List;

public class OpenSubsonicArtistsID3 {

    private String ignoredArticles;
    private List<OpenSubsonicIndexID3> index;

    public String getIgnoredArticles() {
        return ignoredArticles;
    }

    public OpenSubsonicArtistsID3 setIgnoredArticles(String ignoredArticles) {
        this.ignoredArticles = ignoredArticles;
        return this;
    }

    public List<OpenSubsonicIndexID3> getIndex() {
        return index;
    }

    public OpenSubsonicArtistsID3 setIndex(List<OpenSubsonicIndexID3> index) {
        this.index = index;
        return this;
    }
}
