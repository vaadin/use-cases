package com.example.ai;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Tells which AI providers have an API key, so a use case can explain what is
 * missing instead of failing on its first call.
 */
@Component
public class AiProviders {

    private final Environment environment;

    public AiProviders(Environment environment) {
        this.environment = environment;
    }

    public boolean isConfigured(AiProvider provider) {
        return StringUtils
                .hasText(environment.getProperty(provider.keyVariable()));
    }
}
