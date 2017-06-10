package net.dorokhov.pony.web.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import net.dorokhov.pony.installation.domain.Installation;

import static com.google.common.base.Preconditions.checkNotNull;

public final class InstallationDto {
    
    private final String version;

    @JsonCreator
    public InstallationDto(String version) {
        this.version = checkNotNull(version);
    }
    
    public InstallationDto(Installation installation) {
        this(installation.getVersion());
    }

    public String getVersion() {
        return version;
    }
}
