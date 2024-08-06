package net.dorokhov.pony2.api.library.domain;

import static com.google.common.base.Preconditions.checkNotNull;

public final class LibrarySearchQuery {
    
    private final String text;

    public LibrarySearchQuery(String text) {
        this.text = checkNotNull(text);
    }

    public String getText() {
        return text;
    }
    
    public static LibrarySearchQuery of(String text) {
        return new LibrarySearchQuery(text);
    }
}
