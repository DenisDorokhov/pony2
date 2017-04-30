package net.dorokhov.pony.logging.repository;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.logging.domain.LogMessage;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class LogMessageRepositoryTests extends IntegrationTest {

    @Autowired
    private LogMessageRepository logMessageRepository;

    @Test
    public void save() throws Exception {
        LogMessage logMessage = logMessageRepository.save(LogMessage.builder()
                .type(LogMessage.Type.DEBUG)
                .code("someCode")
                .text("someText")
                .build());
        assertThat(logMessageRepository.findOne(logMessage.getId())).isNotNull();
    }

}
