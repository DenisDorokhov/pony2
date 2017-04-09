package net.dorokhov.pony.test;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestConfiguration {

    @Bean
    public FlywayMigrationStrategy migrationStrategy() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}
