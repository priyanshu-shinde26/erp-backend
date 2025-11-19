package com.erp.erpbackend.attendance;

public class AttendanceSummary {

    private String courseId;
    private String studentUid;
    private int totalClasses;
    private int presentCount;
    private int absentCount;
    private double attendancePercentage;

    public AttendanceSummary() {
    }

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
        this.attendancePercentage =
                (totalClasses == 0) ? 0.0 : (presentCount * 100.0 / totalClasses);
    }

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

    public int getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(int totalClasses) {
        this.totalClasses = totalClasses;
    }

    public int getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(int presentCount) {
        this.presentCount = presentCount;
    }

    public int getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(int absentCount) {
        this.absentCount = absentCount;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }
}
