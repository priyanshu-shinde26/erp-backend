package com.erp.erpbackend.ai;

public class NotesSummaryRequest {
    private String noteUrl;         // Cloudinary URL of the note
    private String fileBase64;      // OR base64 content directly
    private String mimeType;        // "application/pdf", "application/vnd.ms-powerpoint", etc.
    private String noteTitle;
    private String role;

    public NotesSummaryRequest() {}

    public String getNoteUrl() { return noteUrl; }
    public void setNoteUrl(String noteUrl) { this.noteUrl = noteUrl; }

    public String getFileBase64() { return fileBase64; }
    public void setFileBase64(String fileBase64) { this.fileBase64 = fileBase64; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getNoteTitle() { return noteTitle; }
    public void setNoteTitle(String noteTitle) { this.noteTitle = noteTitle; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}