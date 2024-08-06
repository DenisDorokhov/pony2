package net.dorokhov.pony2.core.installation.service;

import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class BuildVersionProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final BuildVersion buildVersion;

    public BuildVersionProvider(
            @Value("${build.version}") String version,
            @Value("${build.time}") String date
    ) {
        buildVersion = new BuildVersion(version, dateFromString(date));
        logger.info("Build '{}', '{}'.", buildVersion.getVersion(), buildVersion.getDate());
    }

    public BuildVersion getBuildVersion() {
        return buildVersion;
    }

    private LocalDateTime dateFromString(String date) {
        return LocalDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME);
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
            return MoreObjects.toStringHelper(this)
                    .add("version", version)
                    .add("date", date)
                    .toString();
        }
    }
}
