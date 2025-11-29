package com.erp.erpbackend.assignment;

public class AssignmentStatusDto {

    private String assignmentId;
    private String title;
    private String classId;
    private long dueDate;

    private int totalSubmissions;
    private int distinctStudentsSubmitted;
    private int gradedCount;
    private int ungradedCount;

    public AssignmentStatusDto() {
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public long getDueDate() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

    public int getTotalSubmissions() {
        return totalSubmissions;
    }

    public void setTotalSubmissions(int totalSubmissions) {
        this.totalSubmissions = totalSubmissions;
    }

    public int getDistinctStudentsSubmitted() {
        return distinctStudentsSubmitted;
    }

    public void setDistinctStudentsSubmitted(int distinctStudentsSubmitted) {
        this.distinctStudentsSubmitted = distinctStudentsSubmitted;
    }

    public int getGradedCount() {
        return gradedCount;
    }

    public void setGradedCount(int gradedCount) {
        this.gradedCount = gradedCount;
    }

    public int getUngradedCount() {
        return ungradedCount;
    }

    public void setUngradedCount(int ungradedCount) {
        this.ungradedCount = ungradedCount;
    }
}
