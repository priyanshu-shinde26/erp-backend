package com.erp.erpbackend.timetable;

public class SchedulePeriod {
    public String subject;
    public String teacherId;
    public String startTime;  // "HH:mm"
    public String endTime;    // "HH:mm"

    public SchedulePeriod() {}  // Firebase needs no-arg

    public SchedulePeriod(String subject, String teacherId, String startTime, String endTime) {
        this.subject = subject;
        this.teacherId = teacherId;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
