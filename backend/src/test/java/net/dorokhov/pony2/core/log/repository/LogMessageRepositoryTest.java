package net.dorokhov.pony2.core.log.repository;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.log.domain.LogMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LogMessageRepositoryTest extends IntegrationTest {

    @Autowired
    private LogMessageRepository logMessageRepository;

    @Test
    public void shouldSave() {

        LogMessage logMessage = logMessageRepository.save(new LogMessage()
                .setLevel(LogMessage.Level.INFO)
                .setPattern("someCode")
                .setArguments(List.of("arg1", "arg2"))
                .setText("someText"));

        assertThat(logMessageRepository.findById(logMessage.getId())).hasValueSatisfying(result -> {
            assertThat(result.getId()).isNotNull();
            assertThat(result.getDate()).isNotNull();
            assertThat(result.getLevel()).isEqualTo(LogMessage.Level.INFO);
            assertThat(result.getPattern()).isEqualTo("someCode");
            assertThat(result.getText()).isEqualTo("someText");
            assertThat(result.getArguments()).isEqualTo(List.of("arg1", "arg2"));
        });
    }
}
