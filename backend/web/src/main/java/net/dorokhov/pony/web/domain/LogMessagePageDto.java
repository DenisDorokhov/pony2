package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.log.domain.LogMessage;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

public final class LogMessagePageDto extends PageDto {

    private final List<LogMessageDto> logMessages;

    LogMessagePageDto(int pageIndex, int pageSize, int totalPages, List<LogMessageDto> logMessages) {
        super(pageIndex, pageSize, totalPages);
        this.logMessages = unmodifiableList(logMessages);
    }

    public List<LogMessageDto> getLogMessages() {
        return logMessages;
    }

    public static LogMessagePageDto of(Page<LogMessage> logMessagePage) {
        return new LogMessagePageDto(logMessagePage.getNumber(), logMessagePage.getSize(), logMessagePage.getTotalPages(),
                logMessagePage.getContent().stream()
                        .map(LogMessageDto::of)
                        .collect(Collectors.toList()));
    }
}
