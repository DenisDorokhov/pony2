package net.dorokhov.pony2.core;

import net.dorokhov.pony2.PonyApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EntityScan(basePackageClasses = PonyApplication.class)
@EnableAsync
@EnableScheduling
public class CoreConfig {
}
