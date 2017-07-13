package net.dorokhov.pony.search.domain;

import static com.google.common.base.Preconditions.checkNotNull;

public final class SearchQuery {
    
    private final String text;

    public SearchQuery(String text) {
        this.text = checkNotNull(text);
    }

    public String getText() {
        return text;
    }
    
    public static SearchQuery of(String text) {
        return new SearchQuery(text);
    }
}
