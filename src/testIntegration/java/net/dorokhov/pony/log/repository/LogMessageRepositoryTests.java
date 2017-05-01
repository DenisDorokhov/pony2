package net.dorokhov.pony.log.repository;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.log.domain.LogMessage;
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
                .arguments(ImmutableList.of("arg1", "arg2"))
                .build());
        assertThat(logMessageRepository.findOne(logMessage.getId())).isNotNull();
    }
}
