package net.dorokhov.pony.entity;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LogMessageTests {

    @Test
    public void shouldFailOnNotNullViolation() throws Exception {
        
        assertThatThrownBy(() -> new LogMessage(LogMessage.Type.DEBUG, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LogMessage(null, null)).isInstanceOf(NullPointerException.class);
        
        final LogMessage logMessage = new LogMessage(LogMessage.Type.DEBUG, "");
        assertThatThrownBy(() -> logMessage.setType(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> logMessage.setCode(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> logMessage.setArguments(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void shouldSupportEqualityAndHashCode() throws Exception {

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
