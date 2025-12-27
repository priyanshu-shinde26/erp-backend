package com.erp.erpbackend.attendance;

public class AttendanceRecord {

    // ---------- OLD FIELDS (BACKWARD COMPATIBILITY) ----------
    private String courseId;
    private String studentUid;

    // ---------- NEW FIELDS (UPDATED FLOW) ----------
    private String classId;      // NEW: Replaces courseId for class-based attendance
    private String rollNumber;   // NEW: Primary student identifier

    // ---------- COMMON FIELDS ----------
    private String date;         // yyyy-MM-dd format
    private String status;       // PRESENT / ABSENT
    private String markedBy;     // uid of admin/teacher

    // ---------- CONSTRUCTORS ----------

    public AttendanceRecord() {
        // Required for Firebase deserialization [web:29]
    }


    // ---------- NEW CONSTRUCTOR (CLASS + ROLL NUMBER BASED) ----------
    public AttendanceRecord(String classId,
                            String rollNumber,
                            String date,
                            String status,
                            String markedBy) {
        this.classId = classId;
        this.rollNumber = rollNumber;
        this.date = date;
        this.status = status;
        this.markedBy = markedBy;
    }

    // ---------- GETTERS & SETTERS ----------

    // OLD FIELDS (BACKWARD COMPATIBILITY)
    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getStudentUid() {
        return studentUid;
    }

    public void setStudentUid(String studentUid) {
        this.studentUid = studentUid;
    }

    // NEW FIELDS
    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    // COMMON FIELDS
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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
