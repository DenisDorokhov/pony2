package net.dorokhov.pony.log.repository;

import net.dorokhov.pony.log.domain.LogMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDateTime;

public interface LogMessageRepository extends PagingAndSortingRepository<LogMessage, Long> {

    Page<LogMessage> findByTypeGreaterThanEqual(LogMessage.Type type, Pageable pageable);

    Page<LogMessage> findByTypeGreaterThanEqualAndDateBetween(LogMessage.Type type, LocalDateTime minDate, LocalDateTime maxDate, Pageable pageable);
}
