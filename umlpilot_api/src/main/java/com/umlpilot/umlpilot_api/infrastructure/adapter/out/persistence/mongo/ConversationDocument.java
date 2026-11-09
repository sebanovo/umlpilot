package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("conversations")
public class ConversationDocument {

    @Id
    private String id;
    private String projectId;
    private String title;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastMessageAt;
    private String focusDiagramId;
    private String status;
    private String summaryText;
    private String summaryThroughMessageId;
    private Instant summaryUpdatedAt;
    private long unsummarizedCount;

    public ConversationDocument() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Instant lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public String getFocusDiagramId() { return focusDiagramId; }
    public void setFocusDiagramId(String focusDiagramId) { this.focusDiagramId = focusDiagramId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public String getSummaryThroughMessageId() { return summaryThroughMessageId; }
    public void setSummaryThroughMessageId(String summaryThroughMessageId) {
        this.summaryThroughMessageId = summaryThroughMessageId;
    }
    public Instant getSummaryUpdatedAt() { return summaryUpdatedAt; }
    public void setSummaryUpdatedAt(Instant summaryUpdatedAt) { this.summaryUpdatedAt = summaryUpdatedAt; }
    public long getUnsummarizedCount() { return unsummarizedCount; }
    public void setUnsummarizedCount(long unsummarizedCount) { this.unsummarizedCount = unsummarizedCount; }
}
