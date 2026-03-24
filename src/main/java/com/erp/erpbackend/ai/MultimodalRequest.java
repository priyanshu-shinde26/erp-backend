package com.erp.erpbackend.ai;

import java.util.List;

public class MultimodalRequest {
    private String message;
    private String base64Image;   // optional: base64-encoded image
    private String mimeType;      // e.g. "image/jpeg"
    private List<ChatMessage> history;

    public MultimodalRequest() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getBase64Image() { return base64Image; }
    public void setBase64Image(String base64Image) { this.base64Image = base64Image; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public List<ChatMessage> getHistory() { return history; }
    public void setHistory(List<ChatMessage> history) { this.history = history; }
}