package com.erp.erpbackend.timetable;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService service;

    // CREATE (ADMIN + TEACHER)
    @PostMapping
    public ResponseEntity<TimetableEntryResponse> create(@RequestBody TimetableEntryRequest request) {
        TimetableEntry saved = service.create(request);
        return ResponseEntity.ok(toResponse(saved));
    }

    // UPDATE (ADMIN + TEACHER)
    @PutMapping("/{id}")
    public ResponseEntity<TimetableEntryResponse> update(@PathVariable Long id,
                                                         @RequestBody TimetableEntryRequest request) {
        TimetableEntry updated = service.update(id, request);
        return ResponseEntity.ok(toResponse(updated));
    }

    // DELETE (ADMIN + TEACHER)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // GET all (everyone logged in)
    @GetMapping
    public ResponseEntity<List<TimetableEntryResponse>> getAll() {
        List<TimetableEntryResponse> list = service.getAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // GET by day (everyone logged in)
    @GetMapping("/day/{day}")
    public ResponseEntity<List<TimetableEntryResponse>> getByDay(@PathVariable String day) {
        DayOfWeek d = DayOfWeek.valueOf(day.toUpperCase());
        List<TimetableEntryResponse> list = service.getByDay(d).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    private TimetableEntryResponse toResponse(TimetableEntry e) {
        TimetableEntryResponse r = new TimetableEntryResponse();
        r.setId(e.getId());
        r.setClassName(e.getClassName());
        r.setDayOfWeek(e.getDayOfWeek());
        r.setStartTime(e.getStartTime());
        r.setEndTime(e.getEndTime());
        r.setSubjectName(e.getSubjectName());
        r.setTeacherName(e.getTeacherName());
        r.setClassroom(e.getClassroom());
        return r;
    }
}
