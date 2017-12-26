package net.dorokhov.pony.web.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.classmate.TypeResolver;
import com.google.common.collect.ImmutableSet;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import springfox.documentation.builders.OperationBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiDescription;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ApiListingScannerPlugin;
import springfox.documentation.spi.service.contexts.DocumentationContext;
import springfox.documentation.spring.web.readers.operation.CachingOperationNameGenerator;

import static java.util.Collections.singletonList;

@Component
public class SecurityApiListingScanner implements ApiListingScannerPlugin {

    @Override
    public List<ApiDescription> apply(DocumentationContext context) {
        return new ArrayList<>(singletonList(
                new ApiDescription(
                        "/api/authentication",
                        "",
                        Arrays.asList(
                                new OperationBuilder(new CachingOperationNameGenerator())
                                        .tags(ImmutableSet.of("Security"))
                                        .codegenMethodNameStem("apiAuthenticationPOST")
                                        .method(HttpMethod.POST)
                                        .summary("Log user in.")
                                        .parameters(Arrays.asList(
                                                new ParameterBuilder()
                                                        .description("email")
                                                        .type(new TypeResolver().resolve(String.class))
                                                        .modelRef(new ModelRef("string"))
                                                        .name("email")
                                                        .parameterType("form")
                                                        .build(),
                                                new ParameterBuilder()
                                                        .description("command")
                                                        .type(new TypeResolver().resolve(String.class))
                                                        .modelRef(new ModelRef("string"))
                                                        .name("password")
                                                        .parameterType("form")
                                                        .build()
                                        ))
                                        .build(),
                                new OperationBuilder(new CachingOperationNameGenerator())
                                        .tags(ImmutableSet.of("Security"))
                                        .codegenMethodNameStem("apiAuthenticationDELETE")
                                        .method(HttpMethod.DELETE)
                                        .summary("Log user out.")
                                        .build()
                        ), 
                        false)
        ));
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return DocumentationType.SWAGGER_2.equals(delimiter);
    }
}
