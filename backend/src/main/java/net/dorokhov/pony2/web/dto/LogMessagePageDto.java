package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.log.domain.LogMessage;
import org.springframework.data.domain.Page;

import java.util.List;

public class LogMessagePageDto extends PageDto<LogMessagePageDto> {

    private List<LogMessageDto> logMessages;

    public List<LogMessageDto> getLogMessages() {
        return logMessages;
    }

    public LogMessagePageDto setLogMessages(List<LogMessageDto> logMessages) {
        this.logMessages = logMessages;
        return this;
    }

    public static LogMessagePageDto of(Page<LogMessage> logMessagePage) {
        return new LogMessagePageDto()
                .setPageIndex(logMessagePage.getNumber())
                .setPageSize(logMessagePage.getSize())
                .setTotalPages(logMessagePage.getTotalPages())
                .setLogMessages(logMessagePage.getContent().stream()
                        .map(LogMessageDto::of)
                        .toList());
    }
}
