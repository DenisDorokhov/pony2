package net.dorokhov.pony.build.domain;

import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public class BuildVersion {
    
    private final String version;
    private final LocalDateTime date;

    public BuildVersion(String version, LocalDateTime date) {
        this.version = checkNotNull(version);
        this.date = checkNotNull(date);
    }

    public String getVersion() {
        return version;
    }

    public LocalDateTime getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "BuildVersion{" +
                "version='" + version + '\'' +
                ", date=" + date +
                '}';
    }
}
