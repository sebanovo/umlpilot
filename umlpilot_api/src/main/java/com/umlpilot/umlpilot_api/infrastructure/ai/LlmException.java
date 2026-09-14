package com.umlpilot.umlpilot_api.infrastructure.ai;

import org.springframework.web.client.RestClientResponseException;

public class LlmException extends RuntimeException {

    private final boolean quotaRelated;

    public LlmException(String message, boolean quotaRelated) {
        super(message);
        this.quotaRelated = quotaRelated;
    }

    public boolean isQuotaRelated() {
        return quotaRelated;
    }

    public static LlmException fromHttp(String provider, RestClientResponseException e) {
        int status = e.getStatusCode().value();
        String body = e.getResponseBodyAsString();
        if (body != null && body.length() > 300) {
            body = body.substring(0, 300);
        }
        boolean quota = status == 401 || status == 402 || status == 403 || status == 429;
        String suffix = (body != null && !body.isBlank()) ? " - " + body : "";
        return new LlmException(provider + " HTTP " + status + suffix, quota);
    }
}
