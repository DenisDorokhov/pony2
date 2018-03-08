package net.dorokhov.pony.web.security;

import com.fasterxml.classmate.TypeResolver;
import com.google.common.collect.ImmutableSet;
import net.dorokhov.pony.web.domain.AuthenticationDto;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.UserDto;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import springfox.documentation.builders.OperationBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiDescription;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ApiListingScannerPlugin;
import springfox.documentation.spi.service.contexts.DocumentationContext;
import springfox.documentation.spring.web.readers.operation.CachingOperationNameGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.*;

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
                                        .responseMessages(ImmutableSet.of(
                                                new ResponseMessage(
                                                        200, 
                                                        "User has been logged in.", 
                                                        new ModelRef(AuthenticationDto.class.getSimpleName()), 
                                                        emptyMap(), 
                                                        emptyList()
                                                ),
                                                new ResponseMessage(
                                                        401, 
                                                        "Authentication failed.", 
                                                        new ModelRef(ErrorDto.class.getSimpleName()), 
                                                        emptyMap(), 
                                                        emptyList()
                                                )
                                        ))
                                        .build(),
                                new OperationBuilder(new CachingOperationNameGenerator())
                                        .tags(ImmutableSet.of("Security"))
                                        .codegenMethodNameStem("apiAuthenticationDELETE")
                                        .method(HttpMethod.DELETE)
                                        .summary("Log user out.")
                                        .responseMessages(ImmutableSet.of(
                                                new ResponseMessage(
                                                        200, 
                                                        "User has been logged out.",
                                                        new ModelRef(UserDto.class.getSimpleName()), 
                                                        emptyMap(), 
                                                        emptyList()
                                                ),
                                                new ResponseMessage(
                                                        401, 
                                                        "Authentication failed.",
                                                        new ModelRef(ErrorDto.class.getSimpleName()), 
                                                        emptyMap(), 
                                                        emptyList()
                                                )
                                        ))
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
