package net.dorokhov.pony2.web.dto.opensubsonic;

public class OpenSubsonicBookmark {

    private long position;
    private String username;
    private String comment;
    private String created;
    private String changed;
    private OpenSubsonicChild entry;

    public long getPosition() {
        return position;
    }

    public OpenSubsonicBookmark setPosition(long position) {
        this.position = position;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public OpenSubsonicBookmark setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public OpenSubsonicBookmark setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public OpenSubsonicBookmark setCreated(String created) {
        this.created = created;
        return this;
    }

    public String getChanged() {
        return changed;
    }

    public OpenSubsonicBookmark setChanged(String changed) {
        this.changed = changed;
        return this;
    }

    public OpenSubsonicChild getEntry() {
        return entry;
    }

    public OpenSubsonicBookmark setEntry(OpenSubsonicChild entry) {
        this.entry = entry;
        return this;
    }
}
