package net.dorokhov.pony2.api.log.domain;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class LogMessageTest {

    @Test
    public void shouldSupportEqualityAndHashCode() {

        LogMessage eqLogMessage1 = logMessage().setId("1");
        LogMessage eqLogMessage2 = logMessage().setId("1");
        LogMessage diffLogMessage = logMessage().setId("2");

        assertThat(eqLogMessage1.hashCode()).isEqualTo(eqLogMessage2.hashCode());
        assertThat(eqLogMessage1.hashCode()).isNotEqualTo(diffLogMessage.hashCode());

        assertThat(eqLogMessage1).isEqualTo(eqLogMessage1);
        assertThat(eqLogMessage1).isEqualTo(eqLogMessage2);

        assertThat(eqLogMessage1).isNotEqualTo(diffLogMessage);
        assertThat(eqLogMessage1).isNotEqualTo("foo1");
        assertThat(eqLogMessage1).isNotEqualTo(null);
    }
    
    private LogMessage logMessage() {
        return new LogMessage()
                .setId("1")
                .setDate(LocalDateTime.now())
                .setLevel(LogMessage.Level.DEBUG)
                .setPattern("someCode")
                .setText("someText")
                .setArguments(Lists.newArrayList("foo", "bar"));
    }
}
