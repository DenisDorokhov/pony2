package net.dorokhov.pony;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackageClasses = PonyApplication.class) 
public class PonyApplication {
    public static void main(String[]args) {
        SpringApplication.run(PonyApplication.class, args);
    }
}
