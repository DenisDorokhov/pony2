package net.dorokhov.pony.build;

import net.dorokhov.pony.build.domain.BuildVersion;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class BuildVersionProviderImplTests {

    @Test
    public void parseVersionAndDate() throws Exception {
        BuildVersionProviderImpl buildVersionProvider = new BuildVersionProviderImpl("2.0", "2017-04-23T09:11:17+0200");
        BuildVersion buildVersion = buildVersionProvider.getBuildVersion();
        assertThat(buildVersion.getVersion()).isEqualTo("2.0");
        assertThat(buildVersion.getDate()).isEqualTo(LocalDateTime.of(2017, 4, 23, 9, 11, 17));
    }
}
