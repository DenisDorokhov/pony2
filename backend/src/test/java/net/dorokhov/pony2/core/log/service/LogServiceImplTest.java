package net.dorokhov.pony2.core.log.service;

import net.dorokhov.pony2.api.log.domain.LogMessage;
import net.dorokhov.pony2.core.log.repository.LogMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony2.api.log.domain.LogMessage.Level.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LogServiceImplTest {

    private static final String PATTERN = "foo {}";
    private static final String ARGUMENT = "bar";
    private static final String ERROR_MESSAGE = "error";
    private static final String TEXT = "foo bar";

    private LogServiceImpl logService;

    @Mock
    private LogMessageRepository logMessageRepository;

    @Mock
    private Logger logger;

    @BeforeEach
    void setUp() {
        logService = new LogServiceImpl(logMessageRepository, LogMessage.Level.DEBUG);
    }

    @Test
    void shouldGetByTypeAndDate() {

        Page<LogMessage> page = new PageImpl<>(emptyList());
        when(logMessageRepository.findByLevelInAndDateBetween(any(), any(), any(), any())).thenReturn(page);

        assertThat(logService.getByTypeAndDate(DEBUG, LocalDateTime.now(), LocalDateTime.now(), PageRequest.of(0, 10))).isSameAs(page);
    }

    @Test
    void shouldLogDebugMessage() {

        when(logMessageRepository.save(any())).then(returnsFirstArg());

        RuntimeException error = new RuntimeException(ERROR_MESSAGE);
        checkLogMessage(logService.debug(logger, PATTERN, ARGUMENT, error).orElseThrow(), DEBUG);
        verify(logger).debug(eq(PATTERN), aryEq(new Object[]{ARGUMENT, error}));
    }

    @Test
    void shouldLogInfoMessage() {

        when(logMessageRepository.save(any())).then(returnsFirstArg());

        RuntimeException error = new RuntimeException(ERROR_MESSAGE);
        checkLogMessage(logService.info(logger, PATTERN, ARGUMENT, error).orElseThrow(), INFO);
        verify(logger).info(anyString(), aryEq(new Object[]{ARGUMENT, error}));
    }

    @Test
    void shouldLogWarnMessage() {

        when(logMessageRepository.save(any())).then(returnsFirstArg());

        RuntimeException error = new RuntimeException(ERROR_MESSAGE);
        checkLogMessage(logService.warn(logger, PATTERN, ARGUMENT, error).orElseThrow(), WARN);
        verify(logger).warn(eq(PATTERN), aryEq(new Object[]{ARGUMENT, error}));
    }

    @Test
    void shouldLogErrorMessage() {

        when(logMessageRepository.save(any())).then(returnsFirstArg());

        RuntimeException error = new RuntimeException(ERROR_MESSAGE);
        checkLogMessage(logService.error(logger, PATTERN, ARGUMENT, error).orElseThrow(), ERROR);
        verify(logger).error(eq(PATTERN), aryEq(new Object[]{ARGUMENT, error}));
    }

    private void checkLogMessage(LogMessage logMessage, LogMessage.Level level) {
        assertThat(logMessage.getLevel()).isEqualTo(level);
        assertThat(logMessage.getPattern()).isEqualTo(PATTERN);
        assertThat(logMessage.getArguments()).hasSize(2);
        assertThat(logMessage.getArguments().get(0)).isEqualTo(ARGUMENT);
        assertThat(logMessage.getArguments().get(1)).startsWith("java.lang.RuntimeException: " + ERROR_MESSAGE);
        assertThat(logMessage.getText()).isEqualTo(TEXT);
    }

    @Test
    void shouldIgnoreNotIncludedLevels() {

        logService = new LogServiceImpl(logMessageRepository, INFO);

        assertThat(logService.debug(logger, PATTERN, ARGUMENT)).isEmpty();
        verify(logger).debug(eq(PATTERN), aryEq(new Object[]{ARGUMENT}));
    }
}
