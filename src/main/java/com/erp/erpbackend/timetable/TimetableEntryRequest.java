// src/main/java/com/erp/erpbackend/timetable/TimetableEntryRequest.java
package com.erp.erpbackend.timetable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimetableEntryRequest {
    private String dayOfWeek;   // e.g. "MONDAY"
    private String startTime;   // "09:00"
    private String endTime;     // "10:00"
    private String subjectName; // "Maths"
    private String teacherName; // "Mr. Sharma"
    private String classroom;   // "101"
}
