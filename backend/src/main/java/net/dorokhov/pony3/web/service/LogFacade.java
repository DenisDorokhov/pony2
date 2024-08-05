package net.dorokhov.pony3.web.service;

import net.dorokhov.pony3.api.log.domain.LogMessage.Level;
import net.dorokhov.pony3.api.log.service.LogService;
import net.dorokhov.pony3.web.dto.LogMessagePageDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LogFacade {
    
    private static final int PAGE_SIZE = 30;
    
    private final LogService logService;

    public LogFacade(LogService logService) {
        this.logService = logService;
    }

    @Transactional(readOnly = true)
    public LogMessagePageDto getLog(Level minLevel, int pageIndex, int pageSize) {
        return LogMessagePageDto.of(logService.getByType(minLevel, PageRequest.of(pageIndex, Math.min(PAGE_SIZE, Math.abs(pageSize)), Sort.Direction.DESC, "date")));
    }
    
    @Transactional(readOnly = true)
    public LogMessagePageDto getLog(Level minLevel, LocalDateTime minDate, LocalDateTime maxDate, int pageIndex, int pageSize) {
        return LogMessagePageDto.of(logService.getByTypeAndDate(minLevel, minDate, maxDate,
                PageRequest.of(pageIndex, Math.min(PAGE_SIZE, Math.abs(pageSize)), Sort.Direction.DESC, "date")));
    }
}
