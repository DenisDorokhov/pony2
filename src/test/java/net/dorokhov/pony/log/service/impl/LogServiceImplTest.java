package net.dorokhov.pony.log.service.impl;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.log.domain.LogMessage;
import net.dorokhov.pony.log.repository.LogMessageRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static net.dorokhov.pony.log.domain.LogMessage.Level.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LogServiceImplTest {
    
    private static final String PATTERN = "foo {}";
    private static final String ARGUMENT = "bar";
    private static final String ERROR_MESSAGE = "error";
    private static final String TEXT = "foo bar";

    @InjectMocks
    private LogServiceImpl logService;
    
    @Mock
    private LogMessageRepository logMessageRepository;
    
    @Mock
    private Logger logger;

    @Test
    public void shouldGetByType() throws Exception {
        Page<LogMessage> page = new PageImpl<>(ImmutableList.of());
        given(logMessageRepository.findByLevelGreaterThanEqual(any(), any())).willReturn(page);
        assertThat(logService.getByType(DEBUG, new PageRequest(0, 10))).isSameAs(page);
    }

    @Test
    public void shouldGetByTypeAndDate() throws Exception {
        Page<LogMessage> page = new PageImpl<>(ImmutableList.of());
        given(logMessageRepository.findByLevelGreaterThanEqualAndDateBetween(any(), any(), any(), any())).willReturn(page);
        assertThat(logService.getByTypeAndDate(DEBUG, LocalDateTime.now(), LocalDateTime.now(), new PageRequest(0, 10))).isSameAs(page);
    }

    @Test
    public void shouldLogDebugMessage() throws Exception {
        given(logMessageRepository.save((LogMessage) any())).willAnswer(returnsFirstArg());
        checkLogMessage(logService.debug(logger, PATTERN, ARGUMENT, new RuntimeException(ERROR_MESSAGE)), DEBUG);
        verify(logger).debug(any(), (Object[]) any());
    }
    
    @Test
    public void shouldLogInfoMessage() throws Exception {
        given(logMessageRepository.save((LogMessage) any())).willAnswer(returnsFirstArg());
        checkLogMessage(logService.info(logger, PATTERN, ARGUMENT, new RuntimeException(ERROR_MESSAGE)), INFO);
        verify(logger).info(any(), (Object[]) any());
    }
    
    @Test
    public void shouldLogWarnMessage() throws Exception {
        given(logMessageRepository.save((LogMessage) any())).willAnswer(returnsFirstArg());
        checkLogMessage(logService.warn(logger, PATTERN, ARGUMENT, new RuntimeException(ERROR_MESSAGE)), WARN);
        verify(logger).warn(any(), (Object[]) any());
    }
    
    @Test
    public void shouldLogErrorMessage() throws Exception {
        given(logMessageRepository.save((LogMessage) any())).willAnswer(returnsFirstArg());
        checkLogMessage(logService.error(logger, PATTERN, ARGUMENT, new RuntimeException(ERROR_MESSAGE)), ERROR);
        verify(logger).error(any(), (Object[]) any());
    }
    
    private void checkLogMessage(LogMessage logMessage, LogMessage.Level level) {
        assertThat(logMessage.getLevel()).isEqualTo(level);
        assertThat(logMessage.getPattern()).isEqualTo(PATTERN);
        assertThat(logMessage.getArguments()).hasSize(2);
        assertThat(logMessage.getArguments().get(0)).isEqualTo(ARGUMENT);
        assertThat(logMessage.getArguments().get(1)).startsWith("java.lang.RuntimeException: " + ERROR_MESSAGE);
        assertThat(logMessage.getText()).isEqualTo(TEXT);
    }
}
