package com.erp.erpbackend.controller;

import com.erp.erpbackend.attendance.AttendanceRecord;
import com.erp.erpbackend.attendance.AttendanceSummary;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.database.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private static final Logger logger =
            LoggerFactory.getLogger(StudentController.class);

    private final DatabaseReference studentsRef;
    private final DatabaseReference rolesRootRef;
    public static class StudentProfile {
        public String rollNo;
        public String name;
        public String classId;
        public String email;

        public StudentProfile() {}
    }
    public StudentController() {
        this.studentsRef =
                FirebaseDatabase.getInstance().getReference("students");
        this.rolesRootRef =
                FirebaseDatabase.getInstance().getReference("roles");

        logger.info("StudentController initialized. studentsRef path={}",
                studentsRef.getPath().toString());
    }

    // =========================================================
    // NEW ENDPOINT (ADDED ONLY – OLD CODE UNTOUCHED)
    // =========================================================

    /**
     * GET /api/students
     * Used by TEACHER / ADMIN to load all students (attendance screen)
     */
    @GetMapping
    public ResponseEntity<?> getAllStudents(
            @RequestHeader(value = "Authorization", required = false)
            String authHeader) {

        logger.info("GET /api/students called");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing Authorization header");
        }

        try {
            String idToken = authHeader.substring(7);
            FirebaseToken decoded =
                    FirebaseAuth.getInstance().verifyIdToken(idToken);
            String requesterUid = decoded.getUid();

            // OPTIONAL: role check (recommended, but not breaking)
            // If you already enforce roles elsewhere, this is safe
            final CountDownLatch roleLatch = new CountDownLatch(1);
            final AtomicBoolean allowed = new AtomicBoolean(false);

            rolesRootRef.child(requesterUid)
                    .addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        String role =
                                                String.valueOf(snapshot.getValue());
                                        if ("TEACHER".equalsIgnoreCase(role)
                                                || "ADMIN".equalsIgnoreCase(role)) {
                                            allowed.set(true);
                                        }
                                    }
                                    roleLatch.countDown();
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    roleLatch.countDown();
                                }
                            });

            roleLatch.await(5, TimeUnit.SECONDS);

            if (!allowed.get()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only TEACHER or ADMIN can view students");
            }

            // -------- Fetch all students --------
            final CountDownLatch dataLatch = new CountDownLatch(1);
            final List<Map<String, Object>> students =
                    new ArrayList<>();

            studentsRef.addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            for (DataSnapshot s : snapshot.getChildren()) {
                                Object val = s.getValue();
                                if (val instanceof Map) {
                                    students.add((Map<String, Object>) val);
                                }
                            }
                            dataLatch.countDown();
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            dataLatch.countDown();
                        }
                    });

            dataLatch.await(10, TimeUnit.SECONDS);

            logger.info("Returning {} students", students.size());
            return ResponseEntity.ok(students);

        } catch (Exception e) {
            logger.error("getAllStudents failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to load students: " + e.getMessage());
        }
    }

    // =========================================================
    // OLD ENDPOINTS (UNCHANGED)
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Remove "Bearer "
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decoded.getUid();

            CountDownLatch latch = new CountDownLatch(1);
            final DataSnapshot[] studentHolder = new DataSnapshot[1];

            studentsRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
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

            if (!latch.await(5, TimeUnit.SECONDS)) {
                return ResponseEntity.ok(Map.of("error", "Timeout"));
            }

            if (studentHolder[0] == null || !studentHolder[0].exists()) {
                return ResponseEntity.ok(Map.of(
                        "rollNo", "N/A",
                        "name", "Student",
                        "classId", "N/A"
                ));
            }

            StudentProfile profile = new StudentProfile();
            profile.rollNo = studentHolder[0].child("rollNo").getValue(String.class);
            profile.name = studentHolder[0].child("name").getValue(String.class);
            profile.classId = studentHolder[0].child("classId").getValue(String.class);
            profile.email = decoded.getEmail();

            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "rollNo", "N/A",
                    "name", "Student",
                    "classId", "N/A",
                    "error", "Auth failed"
            ));
        }
    }
    // =========================================================

    // GET /api/students/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getStudentById(
            @PathVariable("id") String id,
            @RequestHeader(value = "Authorization", required = false)
            String authHeader) {

        logger.info("GET /api/students/{} called", id);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing Authorization header");
        }

        String idToken = authHeader.substring(7);
        try {
            FirebaseToken decoded =
                    FirebaseAuth.getInstance().verifyIdToken(idToken);

            final CountDownLatch latch = new CountDownLatch(1);
            final Object[] holder = new Object[1];

            studentsRef.child(id)
                    .addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    holder[0] =
                                            snapshot.exists()
                                                    ? snapshot.getValue()
                                                    : null;
                                    latch.countDown();
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    holder[0] = error;
                                    latch.countDown();
                                }
                            });

            latch.await(10, TimeUnit.SECONDS);

            if (holder[0] == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Student not found");
            }
            if (holder[0] instanceof DatabaseError err) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(err.getMessage());
            }
            return ResponseEntity.ok(holder[0]);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token verification failed");
        }
    }

    // GET /api/students/me
    @GetMapping("/student/me")
    public ResponseEntity<?> getMyAttendance(
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing auth");
        }

        try {
            String token = authHeader.substring(7);
            FirebaseToken decoded =
                    FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decoded.getUid();

            // ---------------- LOAD STUDENT ----------------
            CountDownLatch studentLatch = new CountDownLatch(1);
            final DataSnapshot[] studentHolder = new DataSnapshot[1];

            studentsRef.child(uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            studentHolder[0] = snapshot;
                            studentLatch.countDown();
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            studentLatch.countDown();
                        }
                    });

            studentLatch.await(10, TimeUnit.SECONDS);

            if (studentHolder[0] == null || !studentHolder[0].exists()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            String rollNo =
                    studentHolder[0].child("rollNo").getValue(String.class);
            String classId =
                    studentHolder[0].child("classId").getValue(String.class);

            if (rollNo == null || classId == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            // ---------------- LOAD CLASS ATTENDANCE ----------------
            DatabaseReference classRef =
                    FirebaseDatabase.getInstance()
                            .getReference("attendance/class")
                            .child(classId);

            CountDownLatch attLatch = new CountDownLatch(1);
            final List<AttendanceRecord> result = new ArrayList<>();

            classRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot classSnap) {
                    for (DataSnapshot dateSnap : classSnap.getChildren()) {
                        DataSnapshot rollSnap = dateSnap.child(rollNo);
                        if (rollSnap.exists()) {
                            AttendanceRecord r =
                                    rollSnap.getValue(AttendanceRecord.class);
                            result.add(r);
                        }
                    }
                    attLatch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    attLatch.countDown();
                }
            });

            attLatch.await(10, TimeUnit.SECONDS);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }


    // POST /api/students
    @PostMapping
    public ResponseEntity<?> createOrUpdateStudent(
            @RequestHeader(value = "Authorization", required = false)
            String authHeader,
            @RequestBody Map<String, Object> payload) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing Authorization header");
        }

        try {
            String idToken = authHeader.substring(7);
            FirebaseToken decoded =
                    FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decoded.getUid();

            payload.put("uid", uid);
            studentsRef.child(uid).setValueAsync(payload).get();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(payload);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save student");
        }
    }
    // ===================== NEW : GET STUDENTS BY CLASS =====================
    @GetMapping("/class/{classId}")
    public List<Object> getStudentsByClass(@PathVariable String classId)
            throws InterruptedException {

        DatabaseReference ref =
                FirebaseDatabase.getInstance().getReference("students");

        List<Object> students = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot s : snapshot.getChildren()) {
                    String studentClass =
                            s.child("classId").getValue(String.class);

                    if (classId.equals(studentClass)) {
                        students.add(s.getValue());
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
        return students;
    }
    @GetMapping("/student/me/summary")
    public ResponseEntity<?> getMyAttendanceSummary(
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing auth");
        }

        try {
            String token = authHeader.substring(7);
            FirebaseToken decoded =
                    FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decoded.getUid();

            // ---------------- LOAD STUDENT ----------------
            CountDownLatch studentLatch = new CountDownLatch(1);
            final DataSnapshot[] studentHolder = new DataSnapshot[1];

            studentsRef.child(uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            studentHolder[0] = snapshot;
                            studentLatch.countDown();
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            studentLatch.countDown();
                        }
                    });

            studentLatch.await(10, TimeUnit.SECONDS);

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

            // ---------------- LOAD CLASS ATTENDANCE ----------------
            DatabaseReference classRef =
                    FirebaseDatabase.getInstance()
                            .getReference("attendance/class")
                            .child(classId);

            CountDownLatch attLatch = new CountDownLatch(1);
            final int[] total = {0};
            final int[] present = {0};

            classRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot classSnap) {
                    for (DataSnapshot dateSnap : classSnap.getChildren()) {
                        DataSnapshot rollSnap = dateSnap.child(rollNo);
                        if (rollSnap.exists()) {
                            total[0]++;
                            String status =
                                    rollSnap.child("status").getValue(String.class);
                            if ("PRESENT".equalsIgnoreCase(status)) {
                                present[0]++;
                            }
                        }
                    }
                    attLatch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    attLatch.countDown();
                }
            });

            attLatch.await(10, TimeUnit.SECONDS);

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


}
