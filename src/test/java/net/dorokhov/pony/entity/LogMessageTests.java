package net.dorokhov.pony.entity;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LogMessageTests {

    @Test
    public void supportEqualityAndHashCode() throws Exception {

        LogMessage eqLogMessage1 = new LogMessage(LogMessage.Type.DEBUG, "");
        eqLogMessage1.setId(1L);
        LogMessage eqLogMessage2 = new LogMessage(LogMessage.Type.DEBUG, "");
        eqLogMessage2.setId(1L);
        LogMessage diffLogMessage = new LogMessage(LogMessage.Type.DEBUG, "");
        diffLogMessage.setId(2L);

        assertThat(eqLogMessage1.hashCode()).isEqualTo(eqLogMessage2.hashCode());
        assertThat(eqLogMessage1.hashCode()).isNotEqualTo(diffLogMessage.hashCode());

        assertThat(eqLogMessage1).isEqualTo(eqLogMessage1);
        assertThat(eqLogMessage1).isEqualTo(eqLogMessage2);

        assertThat(eqLogMessage1).isNotEqualTo(diffLogMessage);
        assertThat(eqLogMessage1).isNotEqualTo("foo1");
        assertThat(eqLogMessage1).isNotEqualTo(null);
    }
}
