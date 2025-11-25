package com.erp.erpbackend.timetable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimetableEntryResponse {
    private Long id;
    private String className;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private String subjectName;
    private String teacherName;
    private String classroom;
}
