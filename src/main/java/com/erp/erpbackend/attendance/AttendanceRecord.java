package com.erp.erpbackend.attendance;

public class AttendanceRecord {

    private String courseId;
    private String date;       // yyyy-MM-dd
    private String studentUid;
    private String status;     // PRESENT / ABSENT
    private String markedBy;   // uid of admin/teacher who marked

    public AttendanceRecord() {
        // needed for Firebase
    }

    public AttendanceRecord(String courseId,
                            String date,
                            String studentUid,
                            String status,
                            String markedBy) {
        this.courseId = courseId;
        this.date = date;
        this.studentUid = studentUid;
        this.status = status;
        this.markedBy = markedBy;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStudentUid() {
        return studentUid;
    }

    public void setStudentUid(String studentUid) {
        this.studentUid = studentUid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMarkedBy() {
        return markedBy;
    }

    public void setMarkedBy(String markedBy) {
        this.markedBy = markedBy;
    }
}
