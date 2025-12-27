package com.erp.erpbackend.attendance;

public class AttendanceSummary {

    // ---------- OLD FIELDS (UNCHANGED) ----------
    private String courseId;
    private String studentUid;

    // ---------- NEW FIELD ----------
    // Used for NEW roll-number-based attendance
    private String rollNumber;

    // ---------- STATS ----------
    private int totalClasses;
    private int presentCount;
    private int absentCount;
    private double attendancePercentage;

    public AttendanceSummary() {
        // required for Firebase / Jackson
    }

    // ---------- OLD CONSTRUCTOR (UNCHANGED) ----------
    public AttendanceSummary(String courseId,
                             String studentUid,
                             int totalClasses,
                             int presentCount,
                             int absentCount) {
        this.courseId = courseId;
        this.studentUid = studentUid;
        this.totalClasses = totalClasses;
        this.presentCount = presentCount;
        this.absentCount = absentCount;
        calculatePercentage();
    }

    // ---------- NEW CONSTRUCTOR (ROLL NUMBER BASED) ----------
    public AttendanceSummary(String rollNumber,
                             int totalClasses,
                             int presentCount,
                             int absentCount) {
        this.rollNumber = rollNumber;
        this.totalClasses = totalClasses;
        this.presentCount = presentCount;
        this.absentCount = absentCount;
        calculatePercentage();
    }

    // ---------- HELPER ----------
    private void calculatePercentage() {
        this.attendancePercentage =
                (totalClasses == 0) ? 0.0 : (presentCount * 100.0 / totalClasses);
    }

    // ---------- GETTERS & SETTERS ----------

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

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(int totalClasses) {
        this.totalClasses = totalClasses;
        calculatePercentage();
    }

    public int getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(int presentCount) {
        this.presentCount = presentCount;
        calculatePercentage();
    }

    public int getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(int absentCount) {
        this.absentCount = absentCount;
        calculatePercentage();
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }
}
