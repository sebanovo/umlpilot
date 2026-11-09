package com.umlpilot.umlpilot_api.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ChatMongoPropertiesTest {

    @Test
    void bindsChatConfiguration() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfig.class)
                .withPropertyValues(
                        "app.chat.recent-messages=12",
                        "app.chat.retrieval-limit=3",
                        "app.chat.summary-threshold=80",
                        "app.chat.summary-batch-size=25",
                        "app.chat.summary-interval-ms=90000",
                        "app.chat.index-initialization-enabled=false",
                        "app.chat.timeout-seconds=45")
                .run(context -> {
                    ChatMongoProperties properties = context.getBean(ChatMongoProperties.class);
                    assertEquals(12, properties.getRecentMessages());
                    assertEquals(3, properties.getRetrievalLimit());
                    assertEquals(80, properties.getSummaryThreshold());
                    assertEquals(25, properties.getSummaryBatchSize());
                    assertEquals(90000, properties.getSummaryIntervalMs());
                    assertFalse(properties.isIndexInitializationEnabled());
                    assertEquals(45, properties.getTimeoutSeconds());
                });
    }

    @Configuration
    @EnableConfigurationProperties(ChatMongoProperties.class)
    static class TestConfig {
    }
}
