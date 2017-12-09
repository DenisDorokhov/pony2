package net.dorokhov.pony.installation.service;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:META-INF/build-info.properties")
public class InstallationConfig {
}
