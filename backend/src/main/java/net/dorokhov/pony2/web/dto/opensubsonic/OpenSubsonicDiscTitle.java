package net.dorokhov.pony2.web.dto.opensubsonic;

public class OpenSubsonicDiscTitle {

    private int disc;
    private String title;

    public int getDisc() {
        return disc;
    }

    public OpenSubsonicDiscTitle setDisc(int disc) {
        this.disc = disc;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public OpenSubsonicDiscTitle setTitle(String title) {
        this.title = title;
        return this;
    }
}
