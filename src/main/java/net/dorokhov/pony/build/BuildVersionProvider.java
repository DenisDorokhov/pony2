package net.dorokhov.pony.build;

import net.dorokhov.pony.build.domain.BuildVersion;

public interface BuildVersionProvider {
    BuildVersion getBuildVersion();
}
