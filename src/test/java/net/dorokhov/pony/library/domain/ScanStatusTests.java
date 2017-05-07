package net.dorokhov.pony.library.domain;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScanStatusTests {

    @Test
    public void shouldImplementToString() throws Exception {
        assertThat(new ScanStatus(ScanStatus.Step.FULL_PREPARING, ImmutableList.of(), 0.0).toString()).startsWith("ScanStatus{");
    }
}
