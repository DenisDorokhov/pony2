package net.dorokhov.pony.logging;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.entity.LogMessage;
import net.dorokhov.pony.repository.LogMessageRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.EMPTY_LIST;
import static net.dorokhov.pony.entity.LogMessage.Type.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LogServiceImplTests {

    private static final String CODE = "code";
    private static final String TEXT = "text";
    private static final String DETAILS = "details";
    private static final Throwable THROWABLE = new Exception();
    private static final List<String> ARGUMENTS = ImmutableList.of("arg1", "arg2");
    
    private static final List<LogMessage> LOG_MESSAGES = ImmutableList.of();
    private static final Page<LogMessage> LOG_PAGE = new PageImpl<>(LOG_MESSAGES);
    
    @Mock
    private LogMessageRepository logMessageRepository;
    
    @Mock
    private Logger logger;
    
    @InjectMocks
    private LogServiceImpl logService;

    @Test
    public void getByType() throws Exception {
        given(logMessageRepository.findByTypeGreaterThanEqual(any(), any())).willReturn(LOG_PAGE);
        assertThat(logService.getByType(DEBUG, new PageRequest(0, 10))).isSameAs(LOG_PAGE);
    }

    @Test
    public void getByTypeAndDate() throws Exception {
        given(logMessageRepository.findByTypeGreaterThanEqualAndDateBetween(any(), any(), any(), any())).willReturn(LOG_PAGE);
        assertThat(logService.getByTypeAndDate(DEBUG, LocalDateTime.now(), LocalDateTime.now(), new PageRequest(0, 10))).isSameAs(LOG_PAGE);
    }

    @Test
    public void debugLog() throws Exception {
        
        ArgumentCaptor<LogMessage> savedLog = ArgumentCaptor.forClass(LogMessage.class);

        logService.debug(logger, CODE, TEXT);
        verify(logger, times(1)).debug(any());
        verify(logMessageRepository, times(1)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), DEBUG, null, false);
        
        logService.debug(logger, CODE, ARGUMENTS, TEXT);
        verify(logger, times(2)).debug(any());
        verify(logMessageRepository, times(2)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), DEBUG, ARGUMENTS, false);

        logService.debug(logger, CODE, TEXT, THROWABLE);
        verify(logger, times(3)).debug(any());
        verify(logMessageRepository, times(3)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), DEBUG, null, true);
        
        logService.debug(logger, CODE, ARGUMENTS, TEXT, THROWABLE);
        verify(logger, times(4)).debug(any());
        verify(logMessageRepository, times(4)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), DEBUG, ARGUMENTS, true);

        logService.debug(logger, CODE, TEXT, DETAILS);
        verify(logger, times(5)).debug(any());
        verify(logMessageRepository, times(5)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), DEBUG, null, DETAILS);
        
        logService.debug(logger, CODE, ARGUMENTS, TEXT, DETAILS);
        verify(logger, times(6)).debug(any());
        verify(logMessageRepository, times(6)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), DEBUG, ARGUMENTS, DETAILS);
    }
    
    @Test
    public void infoLog() throws Exception {

        ArgumentCaptor<LogMessage> savedLog = ArgumentCaptor.forClass(LogMessage.class);

        logService.info(logger, CODE, TEXT);
        verify(logger, times(1)).info(any());
        verify(logMessageRepository, times(1)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), INFO, null, false);

        logService.info(logger, CODE, ARGUMENTS, TEXT);
        verify(logger, times(2)).info(any());
        verify(logMessageRepository, times(2)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), INFO, ARGUMENTS, false);

        logService.info(logger, CODE, TEXT, THROWABLE);
        verify(logger, times(3)).info(any());
        verify(logMessageRepository, times(3)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), INFO, null, true);

        logService.info(logger, CODE, ARGUMENTS, TEXT, THROWABLE);
        verify(logger, times(4)).info(any());
        verify(logMessageRepository, times(4)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), INFO, ARGUMENTS, true);

        logService.info(logger, CODE, TEXT, DETAILS);
        verify(logger, times(5)).info(any());
        verify(logMessageRepository, times(5)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), INFO, null, DETAILS);

        logService.info(logger, CODE, ARGUMENTS, TEXT, DETAILS);
        verify(logger, times(6)).info(any());
        verify(logMessageRepository, times(6)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), INFO, ARGUMENTS, DETAILS);
    }
    
    @Test
    public void warnLog() throws Exception {

        ArgumentCaptor<LogMessage> savedLog = ArgumentCaptor.forClass(LogMessage.class);

        logService.warn(logger, CODE, TEXT);
        verify(logger, times(1)).warn(any());
        verify(logMessageRepository, times(1)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), WARN, null, false);

        logService.warn(logger, CODE, ARGUMENTS, TEXT);
        verify(logger, times(2)).warn(any());
        verify(logMessageRepository, times(2)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), WARN, ARGUMENTS, false);

        logService.warn(logger, CODE, TEXT, THROWABLE);
        verify(logger, times(3)).warn(any());
        verify(logMessageRepository, times(3)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), WARN, null, true);

        logService.warn(logger, CODE, ARGUMENTS, TEXT, THROWABLE);
        verify(logger, times(4)).warn(any());
        verify(logMessageRepository, times(4)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), WARN, ARGUMENTS, true);

        logService.warn(logger, CODE, TEXT, DETAILS);
        verify(logger, times(5)).warn(any());
        verify(logMessageRepository, times(5)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), WARN, null, DETAILS);

        logService.warn(logger, CODE, ARGUMENTS, TEXT, DETAILS);
        verify(logger, times(6)).warn(any());
        verify(logMessageRepository, times(6)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), WARN, ARGUMENTS, DETAILS);
    }
    
    @Test
    public void errorLog() throws Exception {

        ArgumentCaptor<LogMessage> savedLog = ArgumentCaptor.forClass(LogMessage.class);

        logService.error(logger, CODE, TEXT);
        verify(logger, times(1)).error(any());
        verify(logMessageRepository, times(1)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), ERROR, null, false);

        logService.error(logger, CODE, ARGUMENTS, TEXT);
        verify(logger, times(2)).error(any());
        verify(logMessageRepository, times(2)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), ERROR, ARGUMENTS, false);

        logService.error(logger, CODE, TEXT, THROWABLE);
        verify(logger, times(3)).error(any());
        verify(logMessageRepository, times(3)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), ERROR, null, true);

        logService.error(logger, CODE, ARGUMENTS, TEXT, THROWABLE);
        verify(logger, times(4)).error(any());
        verify(logMessageRepository, times(4)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), ERROR, ARGUMENTS, true);

        logService.error(logger, CODE, TEXT, DETAILS);
        verify(logger, times(5)).error(any());
        verify(logMessageRepository, times(5)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), ERROR, null, DETAILS);

        logService.error(logger, CODE, ARGUMENTS, TEXT, DETAILS);
        verify(logger, times(6)).error(any());
        verify(logMessageRepository, times(6)).save(savedLog.capture());
        checkLogMessage(savedLog.getValue(), ERROR, ARGUMENTS, DETAILS);
    }
    
    private void checkLogMessage(LogMessage logMessage, LogMessage.Type type, List<String> arguments, boolean detailsPresent) {
        assertThat(logMessage.getType()).isEqualTo(type);
        assertThat(logMessage.getCode()).isEqualTo(CODE);
        assertThat(logMessage.getText()).isEqualTo(TEXT);
        if (detailsPresent) {
            assertThat(logMessage.getDetails()).isPresent();
        } else {
            assertThat(logMessage.getDetails()).isEmpty();
        }
        assertThat(logMessage.getArguments()).isEqualTo(arguments != null ? arguments : EMPTY_LIST);
    }
    
    private void checkLogMessage(LogMessage logMessage, LogMessage.Type type, List<String> arguments, String details) {
        assertThat(logMessage.getType()).isEqualTo(type);
        assertThat(logMessage.getCode()).isEqualTo(CODE);
        assertThat(logMessage.getText()).isEqualTo(TEXT);
        assertThat(logMessage.getDetails()).isEqualTo(Optional.ofNullable(details));
        assertThat(logMessage.getArguments()).isEqualTo(arguments != null ? arguments : EMPTY_LIST);
    }
}
