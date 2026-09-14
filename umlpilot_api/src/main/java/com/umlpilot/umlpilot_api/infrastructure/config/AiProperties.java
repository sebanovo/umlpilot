package com.umlpilot.umlpilot_api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private List<String> providerOrder = new ArrayList<>(List.of("openrouter", "gemini"));
    private Provider openrouter = new Provider();
    private Provider gemini = new Provider();

    public Provider providerConfig(String id) {
        if (id == null) return null;
        return switch (id.toLowerCase()) {
            case "openrouter" -> openrouter;
            case "gemini" -> gemini;
            default -> null;
        };
    }

    public List<String> getProviderOrder() { return providerOrder; }
    public void setProviderOrder(List<String> providerOrder) { this.providerOrder = providerOrder; }
    public Provider getOpenrouter() { return openrouter; }
    public void setOpenrouter(Provider openrouter) { this.openrouter = openrouter; }
    public Provider getGemini() { return gemini; }
    public void setGemini(Provider gemini) { this.gemini = gemini; }

    public static class Provider {
        private List<String> apiKeys = new ArrayList<>();
        private List<String> models = new ArrayList<>();
        private int quotaCooldownSeconds = 300;
        private int errorCooldownSeconds = 30;

        public List<String> getApiKeys() { return apiKeys; }
        public void setApiKeys(List<String> apiKeys) { this.apiKeys = apiKeys; }
        public List<String> getModels() { return models; }
        public void setModels(List<String> models) { this.models = models; }
        public int getQuotaCooldownSeconds() { return quotaCooldownSeconds; }
        public void setQuotaCooldownSeconds(int quotaCooldownSeconds) { this.quotaCooldownSeconds = quotaCooldownSeconds; }
        public int getErrorCooldownSeconds() { return errorCooldownSeconds; }
        public void setErrorCooldownSeconds(int errorCooldownSeconds) { this.errorCooldownSeconds = errorCooldownSeconds; }
    }
}
