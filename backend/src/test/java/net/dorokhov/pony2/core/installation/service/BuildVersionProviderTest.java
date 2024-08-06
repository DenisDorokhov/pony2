package net.dorokhov.pony2.core.installation.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class BuildVersionProviderTest {

    @Test
    public void shouldParseVersionAndDate() {

        BuildVersionProvider buildVersionProvider = new BuildVersionProvider("2.0", "2017-04-23T09:11:17.137823600Z");

        BuildVersionProvider.BuildVersion buildVersion = buildVersionProvider.getBuildVersion();

        assertThat(buildVersion.getVersion()).isEqualTo("2.0");
        assertThat(buildVersion.getDate()).isEqualTo(LocalDateTime.of(2017, 4, 23, 9, 11, 17, 137823600));
    }
}
