package net.dorokhov.pony.entity;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LogMessageTests {

    @Test
    public void supportEqualityAndHashCode() throws Exception {

        LogMessage eqLogMessage1 = LogMessage.builder().id(1L).build();
        LogMessage eqLogMessage2 = LogMessage.builder().id(1L).build();
        LogMessage diffLogMessage = LogMessage.builder().id(2L).build();

        assertThat(eqLogMessage1.hashCode()).isEqualTo(eqLogMessage2.hashCode());
        assertThat(eqLogMessage1.hashCode()).isNotEqualTo(diffLogMessage.hashCode());

        assertThat(eqLogMessage1).isEqualTo(eqLogMessage1);
        assertThat(eqLogMessage1).isEqualTo(eqLogMessage2);

        assertThat(eqLogMessage1).isNotEqualTo(diffLogMessage);
        assertThat(eqLogMessage1).isNotEqualTo("foo1");
        assertThat(eqLogMessage1).isNotEqualTo(null);
    }

    @Test
    public void stringify() throws Exception {
        assertThat(new LogMessage().toString()).startsWith("LogMessage{");
    }
}
