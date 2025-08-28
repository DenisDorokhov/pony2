package net.dorokhov.pony2.web.dto.opensubsonic;

public class OpenSubsonicMusicFolder {

    private int id;
    private String name;

    public int getId() {
        return id;
    }

    public OpenSubsonicMusicFolder setId(int id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public OpenSubsonicMusicFolder setName(String name) {
        this.name = name;
        return this;
    }
}
