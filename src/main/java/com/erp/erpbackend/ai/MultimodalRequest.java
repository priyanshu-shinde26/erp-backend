package com.erp.erpbackend.ai;

public class MultimodalRequest {
    private String message;
    private String fileBase64;      // Base64 encoded file content
    private String mimeType;        // e.g. "application/pdf", "image/png", etc.
    private String fileName;
    private String role;            // "student", "teacher", "admin"

    public MultimodalRequest() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getFileBase64() { return fileBase64; }
    public void setFileBase64(String fileBase64) { this.fileBase64 = fileBase64; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}