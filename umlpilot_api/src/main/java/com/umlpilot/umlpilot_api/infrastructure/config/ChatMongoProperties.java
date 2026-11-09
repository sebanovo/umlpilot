package com.umlpilot.umlpilot_api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración del chat grupal persistente en MongoDB.
 */
@Component
@ConfigurationProperties(prefix = "app.chat")
public class ChatMongoProperties {

    private int recentMessages = 40;
    private int retrievalLimit = 5;
    private int summaryThreshold = 60;
    private int summaryBatchSize = 40;
    private long summaryIntervalMs = 60_000;
    private boolean indexInitializationEnabled = true;
    private int timeoutSeconds = 120;

    public int getRecentMessages() { return recentMessages; }
    public void setRecentMessages(int recentMessages) { this.recentMessages = recentMessages; }

    public int getRetrievalLimit() { return retrievalLimit; }
    public void setRetrievalLimit(int retrievalLimit) { this.retrievalLimit = retrievalLimit; }

    public int getSummaryThreshold() { return summaryThreshold; }
    public void setSummaryThreshold(int summaryThreshold) { this.summaryThreshold = summaryThreshold; }

    public int getSummaryBatchSize() { return summaryBatchSize; }
    public void setSummaryBatchSize(int summaryBatchSize) { this.summaryBatchSize = summaryBatchSize; }

    public long getSummaryIntervalMs() { return summaryIntervalMs; }
    public void setSummaryIntervalMs(long summaryIntervalMs) { this.summaryIntervalMs = summaryIntervalMs; }

    public boolean isIndexInitializationEnabled() { return indexInitializationEnabled; }
    public void setIndexInitializationEnabled(boolean indexInitializationEnabled) {
        this.indexInitializationEnabled = indexInitializationEnabled;
    }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
