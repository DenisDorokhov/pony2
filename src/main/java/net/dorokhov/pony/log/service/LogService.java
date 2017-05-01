package net.dorokhov.pony.log.service;

import net.dorokhov.pony.log.domain.LogMessage;
import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.List;

public interface LogService {

    Page<LogMessage> getByType(LogMessage.Type minimalType, Pageable pageable);
    Page<LogMessage> getByTypeAndDate(LogMessage.Type minimalType, LocalDateTime minDate, LocalDateTime maxDate, Pageable pageable);

    LogMessage debug(Logger logger, String code, String text);
    LogMessage debug(Logger logger, String code, List<String> arguments, String text);

    LogMessage debug(Logger logger, String code, String text, @Nullable Throwable throwable);
    LogMessage debug(Logger logger, String code, List<String> arguments, String text, @Nullable Throwable throwable);

    LogMessage debug(Logger logger, String code, String text, String details);
    LogMessage debug(Logger logger, String code, List<String> arguments, String text, @Nullable String details);

    LogMessage info(Logger logger, String code, String text);
    LogMessage info(Logger logger, String code, List<String> arguments, String text);

    LogMessage info(Logger logger, String code, String text, @Nullable Throwable throwable);
    LogMessage info(Logger logger, String code, List<String> arguments, String text, @Nullable Throwable throwable);

    LogMessage info(Logger logger, String code, String text, @Nullable String details);
    LogMessage info(Logger logger, String code, List<String> arguments, String text, @Nullable String details);

    LogMessage warn(Logger logger, String code, String text);
    LogMessage warn(Logger logger, String code, List<String> arguments, String text);

    LogMessage warn(Logger logger, String code, String text, @Nullable Throwable throwable);
    LogMessage warn(Logger logger, String code, List<String> arguments, String text, @Nullable Throwable throwable);

    LogMessage warn(Logger logger, String code, String text, @Nullable String details);
    LogMessage warn(Logger logger, String code, List<String> arguments, String text, @Nullable String details);

    LogMessage error(Logger logger, String code, String text);
    LogMessage error(Logger logger, String code, List<String> arguments, String text);

    LogMessage error(Logger logger, String code, String text, @Nullable Throwable throwable);
    LogMessage error(Logger logger, String code, List<String> arguments, String text, @Nullable Throwable throwable);

    LogMessage error(Logger logger, String code, String text, @Nullable String details);
    LogMessage error(Logger logger, String code, List<String> arguments, String text, @Nullable String details);
}
