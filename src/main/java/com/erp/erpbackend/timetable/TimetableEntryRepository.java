package com.erp.erpbackend.timetable;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    // we store dayOfWeek as String "MONDAY", "TUESDAY", etc.
    List<TimetableEntry> findByDayOfWeekOrderByStartTime(String dayOfWeek);
}
