package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.log.domain.LogMessage.Level;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.web.dto.LogMessagePageDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.google.common.base.MoreObjects.firstNonNull;

@Service
public class LogFacade {

    private static final int PAGE_SIZE = 30;

    private final LogService logService;

    public LogFacade(LogService logService) {
        this.logService = logService;
    }

    @Transactional(readOnly = true)
    public LogMessagePageDto getLog(Level minLevel, int pageIndex, int pageSize) {
        return LogMessagePageDto.of(logService.getByTypeAndDate(
                firstNonNull(minLevel, Level.INFO),
                PageRequest.of(pageIndex, Math.min(PAGE_SIZE, Math.abs(pageSize)), Sort.Direction.DESC, "date")));
    }
}
