package net.dorokhov.pony.library.domain;

import net.dorokhov.pony.fixture.ScanResultFixtures;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class ScanResultTest {

    @Test
    public void shouldImplementToString() throws Exception {
        assertThat(ScanResultFixtures.builder(ScanType.FULL)
                .id(123L)
                .date(LocalDateTime.now())
                .build().toString()).startsWith("ScanResult{");
    }
}
