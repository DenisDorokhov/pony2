package net.dorokhov.pony2.api.config.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigSetTest {

    @Test
    public void shouldDetectEnabledLlm() {
        assertThat(new ConfigSet(null, List.of(), "http://localhost:11434/v1", "qwen3.5-35b-a3b", null).llmEnabled()).isTrue();
        assertThat(new ConfigSet(null, List.of(), null, "qwen3.5-35b-a3b", null).llmEnabled()).isFalse();
        assertThat(new ConfigSet(null, List.of(), "http://localhost:11434/v1", null, null).llmEnabled()).isFalse();
        assertThat(new ConfigSet(null, List.of(), " ", "qwen3.5-35b-a3b", null).llmEnabled()).isFalse();
        assertThat(new ConfigSet(null, List.of(), "http://localhost:11434/v1", " ", null).llmEnabled()).isFalse();
    }
}
