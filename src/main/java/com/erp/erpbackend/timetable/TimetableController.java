package com.erp.erpbackend.timetable;

import com.google.firebase.database.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/timetable")
public class TimetableController {

    private final FirebaseDatabase firebaseDb = FirebaseDatabase.getInstance();

    // ---------- CLASSES LIST ----------
    @GetMapping("/classes")
    public ResponseEntity<List<String>> getClasses() {
        DatabaseReference studentsRef = firebaseDb.getReference("students");
        CompletableFuture<List<String>> future = new CompletableFuture<>();

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Set<String> unique = new HashSet<>();
                for (DataSnapshot student : snapshot.getChildren()) {
                    String classId = student.child("classId").getValue(String.class);
                    if (classId != null && !classId.trim().isEmpty()) {
                        unique.add(classId.trim());
                    }
                }
                List<String> classes = new ArrayList<>(unique);
                future.complete(classes);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
            }
        });

        try {
            return ResponseEntity.ok(future.get());
        } catch (Exception e) {
            log.error("Get classes failed", e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    // ---------- CREATE PERIOD (NO AUTH) ----------
    @PostMapping("/{classId}/{day}")
    public ResponseEntity<?> createPeriod(
            @PathVariable String classId,
            @PathVariable String day,
            @RequestBody Map<String, Object> periodData
    ) {
        try {
            DatabaseReference dayRef = firebaseDb.getReference("timetable")
                    .child(classId).child(day);
            String periodId = dayRef.push().getKey();

            Map<String, Object> data = new HashMap<>();
            data.put("subject", periodData.getOrDefault("subject", ""));
            data.put("startTime", periodData.getOrDefault("startTime", ""));
            data.put("endTime", periodData.getOrDefault("endTime", ""));
            data.put("teacher", periodData.getOrDefault("teacher", ""));
            data.put("room", periodData.getOrDefault("room", ""));

            dayRef.child(periodId).setValueAsync(data);
            log.info("✅ Created {} for {}/{}", periodId, classId, day);
            return ResponseEntity.ok(Map.of("periodId", periodId, "message", "Created"));
        } catch (Exception e) {
            log.error("Create failed", e);
            return ResponseEntity.status(500).body("Server error");
        }
    }

    // ---------- GET TODAY ----------
    @GetMapping("/{classId}")
    public ResponseEntity<List<Map<String, Object>>> getClassTimetable(@PathVariable String classId) {
        return getDayScheduleData(classId, getTodayDay());
    }

    // ---------- GET SPECIFIC DAY ----------
    @GetMapping("/{classId}/{day}")
    public ResponseEntity<List<Map<String, Object>>> getDaySchedule(
            @PathVariable String classId,
            @PathVariable String day
    ) {
        return getDayScheduleData(classId, day);
    }

    private ResponseEntity<List<Map<String, Object>>> getDayScheduleData(String classId, String day) {
        DatabaseReference dayRef = firebaseDb.getReference("timetable").child(classId).child(day);
        CompletableFuture<List<Map<String, Object>>> future = new CompletableFuture<>();

        dayRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Map<String, Object>> periods = new ArrayList<>();
                for (DataSnapshot periodSnap : snapshot.getChildren()) {
                    Map<String, Object> period = new HashMap<>();
                    period.put("id", periodSnap.getKey());
                    period.put("subject", periodSnap.child("subject").getValue(String.class));
                    period.put("startTime", periodSnap.child("startTime").getValue(String.class));
                    period.put("endTime", periodSnap.child("endTime").getValue(String.class));
                    period.put("teacher", periodSnap.child("teacher").getValue(String.class));
                    period.put("room", periodSnap.child("room").getValue(String.class));
                    periods.add(period);
                }
                future.complete(periods);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
            }
        });

        try {
            return ResponseEntity.ok(future.get());
        } catch (Exception e) {
            log.error("Load schedule failed", e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    // ---------- UPDATE PERIOD ----------
    @PutMapping("/{classId}/{day}/{periodId}")
    public ResponseEntity<String> updatePeriod(
            @PathVariable String classId,
            @PathVariable String day,
            @PathVariable String periodId,
            @RequestBody Map<String, Object> periodData
    ) {
        try {
            DatabaseReference periodRef = firebaseDb.getReference("timetable")
                    .child(classId).child(day).child(periodId);

            Map<String, Object> data = new HashMap<>();
            data.put("subject", periodData.getOrDefault("subject", ""));
            data.put("startTime", periodData.getOrDefault("startTime", ""));
            data.put("endTime", periodData.getOrDefault("endTime", ""));
            data.put("teacher", periodData.getOrDefault("teacher", ""));
            data.put("room", periodData.getOrDefault("room", ""));

            periodRef.setValueAsync(data);
            return ResponseEntity.ok("Updated");
        } catch (Exception e) {
            log.error("Update failed", e);
            return ResponseEntity.status(500).body("Update failed");
        }
    }

    // ---------- DELETE PERIOD ----------
    @DeleteMapping("/{classId}/{day}/{periodId}")
    public ResponseEntity<String> deletePeriod(
            @PathVariable String classId,
            @PathVariable String day,
            @PathVariable String periodId
    ) {
        try {
            DatabaseReference periodRef = firebaseDb.getReference("timetable")
                    .child(classId).child(day).child(periodId);

            periodRef.removeValueAsync();
            return ResponseEntity.ok("Deleted");
        } catch (Exception e) {
            log.error("Delete failed", e);
            return ResponseEntity.status(500).body("Delete failed");
        }
    }

    private String getTodayDay() {
        int dayOfWeek = java.time.DayOfWeek.from(java.time.LocalDate.now()).getValue();
        String[] days = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        return days[dayOfWeek - 1];
    }
}
