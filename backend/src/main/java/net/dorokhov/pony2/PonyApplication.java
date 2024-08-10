package net.dorokhov.pony2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:pony.properties")
@PropertySource(value = "file:${user.home}/.pony2/pony.properties", ignoreResourceNotFound = true)
public class PonyApplication {

	public static void main(String[] args) {
		SpringApplication.run(PonyApplication.class, args);
	}

}
