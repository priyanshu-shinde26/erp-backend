package com.erp.erpbackend.timetable;

import java.util.List;
import java.util.Map;

public class ClassTimetableResponse {
    private String classId;
    private Map<String, List<SchedulePeriod>> days;

    public ClassTimetableResponse() {}

    public ClassTimetableResponse(String classId, Map<String, List<SchedulePeriod>> days) {
        this.classId = classId;
        this.days = days;
    }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public Map<String, List<SchedulePeriod>> getDays() { return days; }
    public void setDays(Map<String, List<SchedulePeriod>> days) { this.days = days; }
}
