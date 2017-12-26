package net.dorokhov.pony.core.log.repository;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.api.log.domain.LogMessage;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class LogMessageRepositoryTest extends IntegrationTest {

    @Autowired
    private LogMessageRepository logMessageRepository;

    @Test
    public void shouldSave() {

        LogMessage logMessage = logMessageRepository.save(LogMessage.builder()
                .type(LogMessage.Level.DEBUG)
                .pattern("someCode")
                .arguments("arg1", "arg2")
                .text("someText")
                .build());

        assertThat(logMessageRepository.findOne(logMessage.getId())).isNotNull();
    }
}
