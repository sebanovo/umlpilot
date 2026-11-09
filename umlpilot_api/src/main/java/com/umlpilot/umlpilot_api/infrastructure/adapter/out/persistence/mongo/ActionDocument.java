package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence.mongo;

public class ActionDocument {

    private String action;
    private String entity;
    private String id;
    private String result;

    public ActionDocument() {
    }

    public ActionDocument(String action, String entity, String id, String result) {
        this.action = action;
        this.entity = entity;
        this.id = id;
        this.result = result;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
