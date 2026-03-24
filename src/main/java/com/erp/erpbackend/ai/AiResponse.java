package com.erp.erpbackend.ai;

public class AiResponse {
    private String reply;
    private String model;
    private boolean success;
    private String error;

    public AiResponse() {}

    public AiResponse(String reply, String model) {
        this.reply = reply;
        this.model = model;
        this.success = true;
    }

    public AiResponse(String error, boolean success) {
        this.error = error;
        this.success = success;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}