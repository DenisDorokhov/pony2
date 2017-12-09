package net.dorokhov.pony.web.service;

import net.dorokhov.pony.log.domain.LogMessage.Level;
import net.dorokhov.pony.log.service.LogService;
import net.dorokhov.pony.web.domain.LogMessagePageDto;
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
    public LogMessagePageDto getLog(Level minLevel, int pageIndex) {
        return LogMessagePageDto.of(logService.getByType(minLevel, new PageRequest(pageIndex, PAGE_SIZE)));
    }
    
    @Override
    @Transactional(readOnly = true)
    public LogMessagePageDto getLog(Level minLevel, LocalDateTime minDate, LocalDateTime maxDate, int pageIndex) {
        return LogMessagePageDto.of(logService.getByTypeAndDate(minLevel, minDate, maxDate,
                new PageRequest(pageIndex, PAGE_SIZE)));
    }
}
