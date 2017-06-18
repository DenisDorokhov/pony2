package net.dorokhov.pony.web.domain;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.log.domain.LogMessage;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

public final class LogDto extends PageDto {
    
    private final List<LogMessageDto> messages;

    public LogDto(int pageIndex, int pageSize, int totalPages, List<LogMessageDto> messages) {
        super(pageIndex, pageSize, totalPages);
        this.messages = ImmutableList.copyOf(messages);
    }

    public List<LogMessageDto> getMessages() {
        return messages;
    }
    
    public static LogDto of(Page<LogMessage> logMessagePage) {
        return new LogDto(logMessagePage.getNumber(), logMessagePage.getSize(), logMessagePage.getTotalPages(), 
                logMessagePage.getContent().stream()
                        .map(LogMessageDto::of)
                        .collect(Collectors.toList()));
    }
}
