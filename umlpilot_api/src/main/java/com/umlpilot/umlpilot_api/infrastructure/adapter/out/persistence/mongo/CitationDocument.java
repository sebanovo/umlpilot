package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence.mongo;

import java.time.Instant;

public class CitationDocument {

    private String messageId;
    private String snippet;
    private String authorName;
    private Instant createdAt;

    public CitationDocument() {
    }

    public CitationDocument(String messageId, String snippet, String authorName, Instant createdAt) {
        this.messageId = messageId;
        this.snippet = snippet;
        this.authorName = authorName;
        this.createdAt = createdAt;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
