package net.dorokhov.pony.repository;

import net.dorokhov.pony.entity.LogMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDateTime;

public interface LogMessageRepository extends PagingAndSortingRepository<LogMessage, Long> {

    Page<LogMessage> findByTypeGreaterThanEqual(LogMessage.Type aType, Pageable aPageable);

    Page<LogMessage> findByTypeGreaterThanEqualAndDateBetween(LogMessage.Type aType, LocalDateTime aMinDate, LocalDateTime aMaxDate, Pageable aPageable);
}
