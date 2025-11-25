package com.erp.erpbackend.timetable;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "timetable_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // optional: which class/section this timetable belongs to ("Class 10-A", etc.)
    private String className;

    // store as String: "MONDAY", "TUESDAY" ...
    private String dayOfWeek;

    private String startTime;   // "09:00"
    private String endTime;     // "10:00"

    private String subjectName; // "Maths"
    private String teacherName; // "Mr. Sharma"

    private String classroom;   // "101"
}
