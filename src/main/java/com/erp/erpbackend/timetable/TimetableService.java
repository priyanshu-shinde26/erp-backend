package com.erp.erpbackend.timetable;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TimetableService {

    private final TimetableEntryRepository repository;

    public TimetableEntry create(TimetableEntryRequest request) {
        TimetableEntry entry = TimetableEntry.builder()
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .subjectName(request.getSubjectName())
                .teacherName(request.getTeacherName())
                .classroom(request.getClassroom())
                .build();

        return repository.save(entry);
    }

    public TimetableEntry update(Long id, TimetableEntryRequest request) {
        TimetableEntry existing = repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Timetable entry not found: " + id));

        existing.setDayOfWeek(request.getDayOfWeek());
        existing.setStartTime(request.getStartTime());
        existing.setEndTime(request.getEndTime());
        existing.setSubjectName(request.getSubjectName());
        existing.setTeacherName(request.getTeacherName());
        existing.setClassroom(request.getClassroom());

        return repository.save(existing);
    }

    public void delete(Long id) {
        TimetableEntry existing = repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Timetable entry not found: " + id));

        repository.delete(existing);
    }

    @Transactional(readOnly = true)
    public List<TimetableEntry> getAll() {
        return repository.findAll(
                Sort.by("dayOfWeek").ascending()
                        .and(Sort.by("startTime").ascending())
        );
    }

    @Transactional(readOnly = true)
    public List<TimetableEntry> getByDay(DayOfWeek dayOfWeek) {
        return repository.findByDayOfWeekOrderByStartTime(dayOfWeek.name());
    }
}
