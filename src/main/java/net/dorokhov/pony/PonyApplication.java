package net.dorokhov.pony;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

@EntityScan(basePackageClasses = {PonyApplication.class, Jsr310JpaConverters.class})
@SpringBootApplication
public class PonyApplication {

    public static void main(String[] args) {
        SpringApplication.run(PonyApplication.class, args);
    }
}
