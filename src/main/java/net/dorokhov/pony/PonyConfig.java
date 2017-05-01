package net.dorokhov.pony;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

@Configuration
@EntityScan(basePackageClasses = {PonyApplication.class, Jsr310JpaConverters.class})
public class PonyConfig {
}
