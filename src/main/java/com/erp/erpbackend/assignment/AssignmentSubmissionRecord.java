package com.erp.erpbackend.assignment;

public class AssignmentSubmissionRecord {

    private String id;
    private String assignmentId;
    private String studentUid;
    private long submittedAt;

    private String fileUrl;        // <-- real https URL from Cloudinary
    private String filePublicId;   // <-- cloudinary public_id

    private Integer marks;
    private String feedback;

    // getters/setters...

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFilePublicId() {
        return filePublicId;
    }

    public void setFilePublicId(String filePublicId) {
        this.filePublicId = filePublicId;
    }
}
