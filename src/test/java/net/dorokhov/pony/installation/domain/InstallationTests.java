package net.dorokhov.pony.installation.domain;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class InstallationTests {

    @Test
    public void stringify() throws Exception {
        assertThat(Installation.builder()
                .id(1L)
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .version("2.0")
                .build().toString()).startsWith("Installation{");
    }
}
