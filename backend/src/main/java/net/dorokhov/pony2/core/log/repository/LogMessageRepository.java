package net.dorokhov.pony2.core.log.repository;

import net.dorokhov.pony2.api.log.domain.LogMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogMessageRepository extends JpaRepository<LogMessage, String> {
    Page<LogMessage> findByLevelIn(List<LogMessage.Level> levels, Pageable pageable);
}
