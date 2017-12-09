package net.dorokhov.pony.fixture;

import net.dorokhov.pony.installation.domain.Installation;

public final class InstallationFixtures {

    private InstallationFixtures() {
    }
    
    public static Installation installation() {
        return installationBuilder().build();
    }
    
    public static Installation.Builder installationBuilder() {
        return Installation.builder().version("2.0");
    }
}
