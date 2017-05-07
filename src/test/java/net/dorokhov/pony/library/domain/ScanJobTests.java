package net.dorokhov.pony.library.domain;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class ScanJobTests {

    @Test
    public void shouldImplementToString() throws Exception {
        assertThat(ScanJob.builder()
                .id(123L)
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .scanType(ScanType.FULL)
                .status(ScanJob.Status.STARTED)
                .logMessage(null)
                .scanResult(null)
                .build().toString()).startsWith("ScanJob{");
    }
}
