package net.dorokhov.pony2.web.dto.opensubsonic;

import java.util.List;

public class OpenSubsonicExtension {

    private String name;
    private List<Integer> versions;

    public String getName() {
        return name;
    }

    public OpenSubsonicExtension setName(String name) {
        this.name = name;
        return this;
    }

    public List<Integer> getVersions() {
        return versions;
    }

    public OpenSubsonicExtension setVersions(List<Integer> versions) {
        this.versions = versions;
        return this;
    }
}
