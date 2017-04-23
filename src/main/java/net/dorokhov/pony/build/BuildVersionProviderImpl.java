package net.dorokhov.pony.build;

import net.dorokhov.pony.build.domain.BuildVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class BuildVersionProviderImpl implements BuildVersionProvider {
    
    private static final String DATE_PATTERN = "u-M-d'T'H:m:sZ";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final BuildVersion buildVersion;

    public BuildVersionProviderImpl(@Value("${build.version}") String version,
                                    @Value("${build.time}") String date) {
        buildVersion = new BuildVersion(version, dateFromString(date));
        log.info("Build '{}', '{}'.", buildVersion.getVersion(), buildVersion.getDate());
    }

    @Override
    public BuildVersion getBuildVersion() {
        return buildVersion;
    }
    
    private LocalDateTime dateFromString(String date) {
        return LocalDateTime.parse(date, DateTimeFormatter.ofPattern(DATE_PATTERN));
    }
}
