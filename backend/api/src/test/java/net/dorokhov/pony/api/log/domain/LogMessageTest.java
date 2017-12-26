package net.dorokhov.pony.api.log.domain;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class LogMessageTest {

    @Test
    public void shouldSupportEqualityAndHashCode() {

        LogMessage eqLogMessage1 = logMessageBuilder().id(1L).build();
        LogMessage eqLogMessage2 = logMessageBuilder().id(1L).build();
        LogMessage diffLogMessage = logMessageBuilder().id(2L).build();

        assertThat(eqLogMessage1.hashCode()).isEqualTo(eqLogMessage2.hashCode());
        assertThat(eqLogMessage1.hashCode()).isNotEqualTo(diffLogMessage.hashCode());

        assertThat(eqLogMessage1).isEqualTo(eqLogMessage1);
        assertThat(eqLogMessage1).isEqualTo(eqLogMessage2);

        assertThat(eqLogMessage1).isNotEqualTo(diffLogMessage);
        assertThat(eqLogMessage1).isNotEqualTo("foo1");
        assertThat(eqLogMessage1).isNotEqualTo(null);
    }
    
    private LogMessage.Builder logMessageBuilder() {
        return LogMessage.builder()
                .id(1L)
                .date(LocalDateTime.now())
                .type(LogMessage.Level.DEBUG)
                .pattern("someCode")
                .text("someText")
                .arguments("foo", "bar");
    }
}
