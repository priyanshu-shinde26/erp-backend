package com.erp.erpbackend.ai;

import java.util.List;

public class AiRequest {
    private String message;
    private List<ChatMessage> history;
    private String model; // optional override

    public AiRequest() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<ChatMessage> getHistory() { return history; }
    public void setHistory(List<ChatMessage> history) { this.history = history; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}