package com.erp.erpbackend.assignment;

public class AssignmentSubmission {

    private String id;
    private String assignmentId;
    private String studentUid;
    private long submittedAt;       // epoch millis
    private String fileUrl;         // Cloudinary secure URL
    private String filePublicId;    // Cloudinary public_id
    private boolean deleted;

    private Integer marks;          // nullable
    private String feedback;        // nullable
    private String gradedByUid;     // nullable
    private Long gradedAt;          // nullable epoch millis

    public AssignmentSubmission() {
    }

    public AssignmentSubmission(String id,
                                String assignmentId,
                                String studentUid,
                                long submittedAt,
                                String fileUrl,
                                String filePublicId,
                                boolean deleted,
                                Integer marks,
                                String feedback,
                                String gradedByUid,
                                Long gradedAt) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.studentUid = studentUid;
        this.submittedAt = submittedAt;
        this.fileUrl = fileUrl;
        this.filePublicId = filePublicId;
        this.deleted = deleted;
        this.marks = marks;
        this.feedback = feedback;
        this.gradedByUid = gradedByUid;
        this.gradedAt = gradedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAssignmentId() { return assignmentId; }
    public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }

    public String getStudentUid() { return studentUid; }
    public void setStudentUid(String studentUid) { this.studentUid = studentUid; }

    public long getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(long submittedAt) { this.submittedAt = submittedAt; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFilePublicId() { return filePublicId; }
    public void setFilePublicId(String filePublicId) { this.filePublicId = filePublicId; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public Integer getMarks() { return marks; }
    public void setMarks(Integer marks) { this.marks = marks; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getGradedByUid() { return gradedByUid; }
    public void setGradedByUid(String gradedByUid) { this.gradedByUid = gradedByUid; }

    public Long getGradedAt() { return gradedAt; }
    public void setGradedAt(Long gradedAt) { this.gradedAt = gradedAt; }
}
