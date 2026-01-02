package com.erp.erpbackend.timetable;

import java.util.List;

public class DayScheduleResponse {
    private String classId;
    private String day;
    private List<SchedulePeriod> periods;

    public DayScheduleResponse() {}

    public DayScheduleResponse(String classId, String day, List<SchedulePeriod> periods) {
        this.classId = classId;
        this.day = day;
        this.periods = periods;
    }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public List<SchedulePeriod> getPeriods() { return periods; }
    public void setPeriods(List<SchedulePeriod> periods) { this.periods = periods; }
}
