package net.dorokhov.pony.installation.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.google.common.base.Preconditions.checkNotNull;

@Service
public class BuildVersionProvider {

    private static final String DATE_PATTERN = "u-M-d'T'H:m:sZ";
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final BuildVersion buildVersion;

    public BuildVersionProvider(@Value("${build.version}") String version,
                                @Value("${build.time}") String date) {
        buildVersion = new BuildVersion(version, dateFromString(date));
        log.info("Build '{}', '{}'.", buildVersion.getVersion(), buildVersion.getDate());
    }

    public BuildVersion getBuildVersion() {
        return buildVersion;
    }

    private LocalDateTime dateFromString(String date) {
        return LocalDateTime.parse(date, DateTimeFormatter.ofPattern(DATE_PATTERN));
    }

    public static class BuildVersion {

        private final String version;
        private final LocalDateTime date;

        public BuildVersion(String version, LocalDateTime date) {
            this.version = checkNotNull(version);
            this.date = checkNotNull(date);
        }

        public String getVersion() {
            return version;
        }

        public LocalDateTime getDate() {
            return date;
        }

        @Override
        public String toString() {
            return "BuildVersion{" +
                    "version='" + version + '\'' +
                    ", date=" + date +
                    '}';
        }
    }
}
