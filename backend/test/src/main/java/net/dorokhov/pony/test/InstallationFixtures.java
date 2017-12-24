package net.dorokhov.pony.test;

import net.dorokhov.pony.api.installation.domain.Installation;

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
