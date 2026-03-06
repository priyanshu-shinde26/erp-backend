package com.erp.erpbackend.ai;

import java.util.List;

public class AiRequest {
    private String message;
    private String role; // "student", "teacher", "admin"
    private List<ChatMessage> conversationHistory;

    public AiRequest() {}

    public AiRequest(String message, String role, List<ChatMessage> conversationHistory) {
        this.message = message;
        this.role = role;
        this.conversationHistory = conversationHistory;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public List<ChatMessage> getConversationHistory() { return conversationHistory; }
    public void setConversationHistory(List<ChatMessage> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }
}