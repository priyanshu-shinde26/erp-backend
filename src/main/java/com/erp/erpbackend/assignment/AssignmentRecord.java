package com.erp.erpbackend.assignment;

public class AssignmentRecord {

    private String id;
    private String title;
    private String description;
    private String classId;
    private String subject;
    private long createdAt;
    private long dueDate;
    private String createdByUid;
    private boolean active;

    // Question file fields
    private String questionFileUrl;       // <-- real https URL from Cloudinary
    private String questionFilePublicId;  // <-- cloudinary public_id, for delete/replace

    // getters/setters...

    public String getQuestionFileUrl() {
        return questionFileUrl;
    }

    public void setQuestionFileUrl(String questionFileUrl) {
        this.questionFileUrl = questionFileUrl;
    }

    public String getQuestionFilePublicId() {
        return questionFilePublicId;
    }

    public void setQuestionFilePublicId(String questionFilePublicId) {
        this.questionFilePublicId = questionFilePublicId;
    }
}
