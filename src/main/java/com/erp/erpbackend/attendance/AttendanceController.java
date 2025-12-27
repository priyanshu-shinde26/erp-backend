package com.erp.erpbackend.attendance;

import com.erp.erpbackend.service.RoleService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.database.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final DatabaseReference studentsRef =
            FirebaseDatabase.getInstance().getReference("students");
    private static final Logger log = LoggerFactory.getLogger(AttendanceController.class);

    private final AttendanceService attendanceService;
    private final RoleService roleService;

    private final DatabaseReference attendanceRoot =
            FirebaseDatabase.getInstance()
                    .getReference("attendance")
                    .child("ROLL_NUMBER");

    public AttendanceController(AttendanceService attendanceService,
                                RoleService roleService) {
        this.attendanceService = attendanceService;
        this.roleService = roleService;
    }

    // ===================== HELPERS =====================

    private String getCurrentUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        return (principal instanceof String s) ? s : null;
    }

    private boolean isPrivileged(String role) {
        if (role == null) return false;
        role = role.toUpperCase();
        return role.equals("ADMIN") || role.equals("TEACHER");
    }

    // ===================== NEW: CLASS MODEL =====================
    public static class ClassModel {
        public String classId;
        public String name;
        public String course;
        public String year;

        public ClassModel() {}

        public ClassModel(String classId, String name, String course, String year) {
            this.classId = classId;
            this.name = name;
            this.course = course;
            this.year = year;
        }
    }

    // ===================== NEW: /api/classes ENDPOINT =====================

    // ===================== MARK ATTENDANCE =====================

    public static class MarkAttendanceRequest {
        private String rollNumber;
        private String status;

        public String getRollNumber() { return rollNumber; }
        public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class ClassAttendanceRequest {
        public String classId;
        public String rollNumber;
        public String status;

        // Getters for proper JSON binding
        public String getClassId() { return classId; }
        public String getRollNumber() { return rollNumber; }
        public String getStatus() { return status; }

        // Setters
        public void setClassId(String classId) { this.classId = classId; }
        public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
        public void setStatus(String status) { this.status = status; }
    }

    @PostMapping("/mark")
    public ResponseEntity<?> mark(@RequestBody MarkAttendanceRequest req) {

        String uid = getCurrentUid();
        if (uid == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String role = roleService.getRoleForUid(uid);
        if (!isPrivileged(role))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        String today = LocalDate.now().toString();

        attendanceService.markAttendanceByRollNumber(
                req.getRollNumber(),
                today,
                req.getStatus(),
                uid
        );

        return ResponseEntity.ok(
                Map.of("message", "Attendance marked", "date", today)
        );
    }

    @PostMapping("/class/mark")
    public ResponseEntity<?> markAttendanceForClass(
            @RequestBody ClassAttendanceRequest req) {

        String uid = getCurrentUid();
        if (uid == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String role = roleService.getRoleForUid(uid);
        if (!isPrivileged(role))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        String today = LocalDate.now().toString();

        attendanceService.markAttendanceByClass(
                req.classId,
                req.rollNumber,
                today,
                req.status,
                uid
        );

        return ResponseEntity.ok("Attendance marked successfully for " + req.rollNumber);
    }

    // ===================== STUDENT VIEW (FIXED) =====================

    /**
     * RETURNS: List<AttendanceRecord>
     * Firebase path:
     * attendance/ROLL_NUMBER/{date}/{rollNumber}
     */
    @GetMapping("/roll/{rollNumber}")
    public ResponseEntity<?> getAttendanceByRollNumber(
            @PathVariable String rollNumber) {

        List<AttendanceRecord> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        attendanceRoot.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot rootSnap) {
                for (DataSnapshot dateSnap : rootSnap.getChildren()) {
                    DataSnapshot rollSnap = dateSnap.child(rollNumber);
                    if (rollSnap.exists()) {
                        AttendanceRecord r =
                                rollSnap.getValue(AttendanceRecord.class);
                        result.add(r);
                    }
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Timeout"));
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/class/{classId}/{date}")
    public ResponseEntity<?> getAttendanceForClass(
            @PathVariable String classId,
            @PathVariable String date) {

        List<AttendanceRecord> records = attendanceService.getAttendanceForClass(classId, date);
        return ResponseEntity.ok(records);
    }

    // ===================== NEW STUDENT ENDPOINTS =====================

    @GetMapping("/student/me")
    public ResponseEntity<?> getMyAttendance(
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.substring(7);
            FirebaseToken decoded =
                    FirebaseAuth.getInstance().verifyIdToken(token);

            String uid = decoded.getUid();

            // ---------- LOAD STUDENT ----------
            CountDownLatch latch = new CountDownLatch(1);
            final DataSnapshot[] studentHolder = new DataSnapshot[1];

            studentsRef.child(uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            studentHolder[0] = snapshot;
                            latch.countDown();
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            latch.countDown();
                        }
                    });

            latch.await(10, TimeUnit.SECONDS);

            if (studentHolder[0] == null || !studentHolder[0].exists()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            String rollNo =
                    studentHolder[0].child("rollNo").getValue(String.class);
            String classId =
                    studentHolder[0].child("classId").getValue(String.class);

            if (rollNo == null || classId == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            return ResponseEntity.ok(
                    attendanceService.getAttendanceForStudentFromClass(
                            classId,
                            rollNo
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }


    @GetMapping("/student/me/summary")
    public ResponseEntity<?> getMyAttendanceSummary(
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.substring(7);
            FirebaseToken decoded =
                    FirebaseAuth.getInstance().verifyIdToken(token);

            String uid = decoded.getUid();

            CountDownLatch latch = new CountDownLatch(1);
            final DataSnapshot[] studentHolder = new DataSnapshot[1];

            studentsRef.child(uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            studentHolder[0] = snapshot;
                            latch.countDown();
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            latch.countDown();
                        }
                    });

            latch.await(10, TimeUnit.SECONDS);

            if (studentHolder[0] == null || !studentHolder[0].exists()) {
                return ResponseEntity.ok(new AttendanceSummary());
            }

            String rollNo =
                    studentHolder[0].child("rollNo").getValue(String.class);
            String classId =
                    studentHolder[0].child("classId").getValue(String.class);

            if (rollNo == null || classId == null) {
                return ResponseEntity.ok(new AttendanceSummary());
            }

            return ResponseEntity.ok(
                    attendanceService.getSummaryForStudentFromClass(
                            classId,
                            rollNo
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.ok(new AttendanceSummary());
        }
    }
    // ===================== SUMMARY (FIXED) =====================

    @GetMapping("/roll/{rollNumber}/summary")
    public ResponseEntity<?> getSummaryByRollNumber(
            @PathVariable String rollNumber) {

        CountDownLatch latch = new CountDownLatch(1);

        final int[] present = {0};
        final int[] total = {0};

        attendanceRoot.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot rootSnap) {
                for (DataSnapshot dateSnap : rootSnap.getChildren()) {
                    DataSnapshot rollSnap = dateSnap.child(rollNumber);
                    if (rollSnap.exists()) {
                        total[0]++;
                        String status =
                                rollSnap.child("status").getValue(String.class);
                        if ("PRESENT".equalsIgnoreCase(status)) {
                            present[0]++;
                        }
                    }
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Timeout"));
        }

        int absent = total[0] - present[0];
        double percent =
                total[0] == 0 ? 0 : (present[0] * 100.0 / total[0]);

        AttendanceSummary summary =
                new AttendanceSummary(
                        rollNumber,
                        total[0],
                        present[0],
                        absent
                );

        summary.setAttendancePercentage(percent);

        return ResponseEntity.ok(summary);
    }

    // =====================================================
// TEACHER: VIEW SUMMARY OF ONE STUDENT (CLASS-BASED)
// URL: /api/attendance/teacher/student/summary
// =====================================================
    @GetMapping("/teacher/student/summary")
    public ResponseEntity<?> getStudentSummaryForTeacher(
            @RequestParam("classId") String classId,
            @RequestParam("rollNumber") String rollNumber,
            @RequestHeader("Authorization") String authHeader) {

        try {
            // (Optional) verify teacher token
            String token = authHeader.substring(7);
            FirebaseAuth.getInstance().verifyIdToken(token);

            DatabaseReference classRef =
                    FirebaseDatabase.getInstance()
                            .getReference("attendance")
                            .child("class")
                            .child(classId);

            CountDownLatch latch = new CountDownLatch(1);
            int[] total = {0};
            int[] present = {0};

            classRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot classSnap) {

                    for (DataSnapshot dateSnap : classSnap.getChildren()) {
                        DataSnapshot rollSnap = dateSnap.child(rollNumber);
                        if (rollSnap.exists()) {
                            total[0]++;
                            String status =
                                    rollSnap.child("status").getValue(String.class);
                            if ("PRESENT".equalsIgnoreCase(status)) {
                                present[0]++;
                            }
                        }
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    latch.countDown();
                }
            });

            latch.await(10, TimeUnit.SECONDS);

            AttendanceSummary summary = new AttendanceSummary();
            summary.setTotalClasses(total[0]);
            summary.setPresentCount(present[0]);
            summary.setAbsentCount(total[0] - present[0]);
            summary.setAttendancePercentage(
                    total[0] == 0 ? 0 : (present[0] * 100.0 / total[0])
            );

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            return ResponseEntity.ok(new AttendanceSummary());
        }
    }

    // ===================== OLD APIs (UNCHANGED) =====================


}
