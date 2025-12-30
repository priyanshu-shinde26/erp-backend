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

    // 🔥 NEW: Per-student view fields (not stored in assignments node)
    private Integer marks;      // Student's marks for this assignment
    private String feedback;    // Teacher feedback for this student

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

    // ---------------- getters / setters ----------------

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

    // 🔥 NEW getters/setters for grade fields

    public Integer getMarks() { return marks; }
    public void setMarks(Integer marks) { this.marks = marks; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    // ---------------- NEW LOGIC (IMPORTANT) ----------------

    /**
     * Returns true if current time is after due date
     */
    public boolean isClosedByDate() {
        return System.currentTimeMillis() > this.dueDate;
    }

    /**
     * Final rule for student submission
     * - assignment must be active
     * - current time must be before due date
     */
    public boolean isSubmissionAllowed() {
        return active && !isClosedByDate();
    }

    /**
     * 🔥 Clone this assignment (used to attach marks/feedback per-student
     *    without modifying the Firebase stored object).
     */
    public Assignment clone() {
        Assignment copy = new Assignment(
                this.id,
                this.title,
                this.description,
                this.classId,
                this.subject,
                this.createdAt,
                this.dueDate,
                this.createdByUid,
                this.active
        );
        copy.setQuestionFileUrl(this.questionFileUrl);
        copy.setQuestionFilePublicId(this.questionFilePublicId);
        // marks/feedback are set later in controller
        return copy;
    }
}
