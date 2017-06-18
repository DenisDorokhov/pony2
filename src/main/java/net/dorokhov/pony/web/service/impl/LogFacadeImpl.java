package net.dorokhov.pony.web.service.impl;

import net.dorokhov.pony.log.service.LogService;
import net.dorokhov.pony.web.domain.LogDto;
import net.dorokhov.pony.web.domain.LogMessageDto;
import net.dorokhov.pony.web.service.LogFacade;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LogFacadeImpl implements LogFacade {
    
    private static final int PAGE_SIZE = 30;
    
    private final LogService logService;

    public LogFacadeImpl(LogService logService) {
        this.logService = logService;
    }

    @Override
    @Transactional(readOnly = true)
    public LogDto getLog(LogMessageDto.Level minLevel, int pageIndex) {
        return LogDto.of(logService.getByType(minLevel.convert(), new PageRequest(pageIndex, PAGE_SIZE)));
    }
    
    @Override
    @Transactional(readOnly = true)
    public LogDto getLog(LogMessageDto.Level minLevel, LocalDateTime minDate, LocalDateTime maxDate, int pageIndex) {
        return LogDto.of(logService.getByTypeAndDate(minLevel.convert(), minDate, maxDate,
                new PageRequest(pageIndex, PAGE_SIZE)));
    }
}
