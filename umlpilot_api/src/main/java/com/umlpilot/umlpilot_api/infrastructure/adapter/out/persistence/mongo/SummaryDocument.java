package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("summaries")
public class SummaryDocument {

    @Id
    private String id;
    private String conversationId;
    private String fromMessageId;
    private String toMessageId;
    private String text;
    private String model;
    private Instant createdAt;

    public SummaryDocument() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getFromMessageId() { return fromMessageId; }
    public void setFromMessageId(String fromMessageId) { this.fromMessageId = fromMessageId; }
    public String getToMessageId() { return toMessageId; }
    public void setToMessageId(String toMessageId) { this.toMessageId = toMessageId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
