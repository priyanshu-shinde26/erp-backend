package com.erp.erpbackend.assignment;

public class Assignment {

    private String id;
    private String title;
    private String description;
    private String classId;       // e.g. "CSE-2"
    private String subject;       // e.g. "JAVA"
    private long createdAt;       // epoch millis
    private long dueDate;         // epoch millis
    private String createdByUid;  // teacher/admin UID
    private boolean active;

    // Teacher’s question PDF
    private String questionFileUrl;      // Cloudinary secure URL
    private String questionFilePublicId; // Cloudinary public_id

    public Assignment() {
    }

    public Assignment(String id,
                      String title,
                      String description,
                      String classId,
                      String subject,
                      long createdAt,
                      long dueDate,
                      String createdByUid,
                      boolean active) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.classId = classId;
        this.subject = subject;
        this.createdAt = createdAt;
        this.dueDate = dueDate;
        this.createdByUid = createdByUid;
        this.active = active;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }

    public String getCreatedByUid() { return createdByUid; }
    public void setCreatedByUid(String createdByUid) { this.createdByUid = createdByUid; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getQuestionFileUrl() { return questionFileUrl; }
    public void setQuestionFileUrl(String questionFileUrl) { this.questionFileUrl = questionFileUrl; }

    public String getQuestionFilePublicId() { return questionFilePublicId; }
    public void setQuestionFilePublicId(String questionFilePublicId) { this.questionFilePublicId = questionFilePublicId; }
}
