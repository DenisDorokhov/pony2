package net.dorokhov.pony2.common;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class TextTemplate {

    private static final VelocityEngine ENGINE = createEngine();

    private static VelocityEngine createEngine() {
        VelocityEngine engine = new VelocityEngine();
        engine.setProperty("runtime.strict_mode.enable", true);
        engine.setProperty("parser.space_gobbling", "none");
        engine.init();
        return engine;
    }

    private final String source;

    public TextTemplate(Resource resource) throws IOException {
        source = resource.getContentAsString(UTF_8);
    }

    public String render() {
        return render(Map.of());
    }

    public String render(Map<String, ?> variables) {
        VelocityContext context = new VelocityContext();
        variables.forEach(context::put);
        StringWriter result = new StringWriter();
        try {
            ENGINE.evaluate(context, result, "LLM prompt", source);
        } catch (VelocityException e) {
            throw new IllegalArgumentException("Invalid prompt template: " + e.getMessage(), e);
        }
        return result.toString();
    }
}
