package net.dorokhov.pony3.core.log.repository;

import net.dorokhov.pony3.api.log.domain.LogMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface LogMessageRepository extends JpaRepository<LogMessage, String> {

    Page<LogMessage> findByLevelGreaterThanEqual(LogMessage.Level level, Pageable pageable);
    Page<LogMessage> findByLevelGreaterThanEqualAndDateBetween(LogMessage.Level level, LocalDateTime minDate, LocalDateTime maxDate, Pageable pageable);
}
