package com.erp.erpbackend.LMS;

public class Note {
    public String id;
    public String title;
    public String subject;
    public String url;
    public String filename;      // ✅ "notes.pdf"
    public long timestamp;       // ✅ long (millis)
    public String uploadedBy;
    public String uploadedByName;

    public Note() {}  // ✅ Required for Firebase

    // Getters/Setters (optional but recommended)
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setUrl(String url) { this.url = url; }
    public void setFilename(String filename) { this.filename = filename; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    // ✅ FIXED HERE
    public void setUploadedByName(String uploadedByName) { this.uploadedByName = uploadedByName; }
}
