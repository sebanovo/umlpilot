package com.umlpilot.umlpilot_api.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeGenerationPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsConfiguredValues() {
        runner.withPropertyValues(
                        "app.code-generation.max-classes=42",
                        "app.code-generation.base-package=com.acme.generated")
                .run(context -> {
                    CodeGenerationProperties properties = context.getBean(CodeGenerationProperties.class);
                    assertEquals(42, properties.getMaxClasses());
                    assertEquals("com.acme.generated", properties.getBasePackage());
                });
    }

    @Test
    void appliesDefaults() {
        runner.run(context -> {
            CodeGenerationProperties properties = context.getBean(CodeGenerationProperties.class);
            assertEquals(100, properties.getMaxClasses());
            assertEquals("com.generated", properties.getBasePackage());
        });
    }

    @Configuration
    @EnableConfigurationProperties(CodeGenerationProperties.class)
    static class TestConfig {
    }
}
