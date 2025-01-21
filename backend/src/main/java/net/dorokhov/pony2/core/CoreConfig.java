package net.dorokhov.pony2.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import net.dorokhov.pony2.PonyApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EntityScan(basePackageClasses = PonyApplication.class)
@EnableAsync
@EnableScheduling
public class CoreConfig implements Jackson2ObjectMapperBuilderCustomizer {

    @Override
    public void customize(Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder) {
        jacksonObjectMapperBuilder
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .modules(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES), new JavaTimeModule());
    }
}
