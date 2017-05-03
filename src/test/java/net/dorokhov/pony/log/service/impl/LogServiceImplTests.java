package net.dorokhov.pony.log.service.impl;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.log.domain.LogMessage;
import net.dorokhov.pony.log.repository.LogMessageRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static net.dorokhov.pony.log.domain.LogMessage.Level.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class LogServiceImplTests {
    
    private static final String PATTERN = "foo {}";
    private static final String ARGUMENT = "bar";
    private static final String ERROR_MESSAGE = "error";
    private static final String TEXT = "foo bar";

    @InjectMocks
    private LogServiceImpl logService;
    
    @Mock
    private LogMessageRepository logMessageRepository;

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    @Before
    public void setUp() throws Exception {
        logService.setSelf(logService);
    }

    @Test
    public void getByType() throws Exception {
        Page<LogMessage> page = new PageImpl<>(ImmutableList.of());
        given(logMessageRepository.findByLevelGreaterThanEqual(any(), any())).willReturn(page);
        assertThat(logService.getByType(DEBUG, new PageRequest(0, 10))).isSameAs(page);
    }

    @Test
    public void getByTypeAndDate() throws Exception {
        Page<LogMessage> page = new PageImpl<>(ImmutableList.of());
        given(logMessageRepository.findByLevelGreaterThanEqualAndDateBetween(any(), any(), any(), any())).willReturn(page);
        assertThat(logService.getByTypeAndDate(DEBUG, LocalDateTime.now(), LocalDateTime.now(), new PageRequest(0, 10))).isSameAs(page);
    }

    @Test
    public void debugLog() throws Exception {
        given(logMessageRepository.save((LogMessage) any())).willAnswer(invocation -> invocation.getArgument(0));
        checkLogMessage(logService.debug(PATTERN, ARGUMENT, new RuntimeException(ERROR_MESSAGE)), DEBUG);
        assertThat(outputCapture.toString()).contains(" DEBUG " + getClass().getName() + " ");
    }
    
    @Test
    public void infoLog() throws Exception {
        given(logMessageRepository.save((LogMessage) any())).willAnswer(invocation -> invocation.getArgument(0));
        checkLogMessage(logService.info(PATTERN, ARGUMENT, new RuntimeException(ERROR_MESSAGE)), INFO);
        assertThat(outputCapture.toString()).contains(" INFO " + getClass().getName() + " ");
    }
    
    @Test
    public void warnLog() throws Exception {
        given(logMessageRepository.save((LogMessage) any())).willAnswer(invocation -> invocation.getArgument(0));
        checkLogMessage(logService.warn(PATTERN, ARGUMENT, new RuntimeException(ERROR_MESSAGE)), WARN);
        assertThat(outputCapture.toString()).contains(" WARN " + getClass().getName() + " ");
    }
    
    @Test
    public void errorLog() throws Exception {
        given(logMessageRepository.save((LogMessage) any())).willAnswer(invocation -> invocation.getArgument(0));
        checkLogMessage(logService.error(PATTERN, ARGUMENT, new RuntimeException(ERROR_MESSAGE)), ERROR);
        assertThat(outputCapture.toString()).contains(" ERROR " + getClass().getName() + " ");
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
