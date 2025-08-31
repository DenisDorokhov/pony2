package net.dorokhov.pony2.web.dto.opensubsonic;

import java.util.List;

public class OpenSubsonicIndexes {

    private List<?> shortcut;
    private List<?> index;
    private List<?> child;

    public List<?> getShortcut() {
        return shortcut;
    }

    public OpenSubsonicIndexes setShortcut(List<?> shortcut) {
        this.shortcut = shortcut;
        return this;
    }

    public List<?> getIndex() {
        return index;
    }

    public OpenSubsonicIndexes setIndex(List<?> index) {
        this.index = index;
        return this;
    }

    public List<?> getChild() {
        return child;
    }

    public OpenSubsonicIndexes setChild(List<?> child) {
        this.child = child;
        return this;
    }
}
