package com.erp.erpbackend.ai;

public class AiResponse {
    private String reply;
    private boolean success;
    private String error;

    public AiResponse() {}

    public AiResponse(String reply, boolean success) {
        this.reply = reply;
        this.success = success;
    }

    public AiResponse(String reply, boolean success, String error) {
        this.reply = reply;
        this.success = success;
        this.error = error;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}