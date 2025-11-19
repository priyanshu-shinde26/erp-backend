package com.erp.erpbackend.attendance;

import com.erp.erpbackend.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private static final Logger log = LoggerFactory.getLogger(AttendanceController.class);

    private final AttendanceService attendanceService;
    private final RoleService roleService;

    public AttendanceController(AttendanceService attendanceService,
                                RoleService roleService) {
        this.attendanceService = attendanceService;
        this.roleService = roleService;
    }

    // ---------- Helpers ----------

    private String getCurrentUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            log.warn("getCurrentUid(): no Authentication in SecurityContext");
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof String s) {
            return s;
        }
        log.warn("getCurrentUid(): principal is not String, was: {}", principal);
        return null;
    }

    // ADMIN or TEACHER are "privileged"
    private boolean isPrivileged(String role) {
        if (role == null) return false;
        String r = role.toUpperCase();
        return r.equals("ADMIN") || r.equals("TEACHER");
    }

    // ---------- DTO ----------

    public static class MarkAttendanceRequest {
        private String studentUid;
        private String courseId;
        private String date;   // yyyy-MM-dd
        private String status; // PRESENT / ABSENT

        public String getStudentUid() {
            return studentUid;
        }

        public void setStudentUid(String studentUid) {
            this.studentUid = studentUid;
        }

        public String getCourseId() {
            return courseId;
        }

        public void setCourseId(String courseId) {
            this.courseId = courseId;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    // ---------- POST: mark attendance (ADMIN / TEACHER only) ----------

    @PostMapping("/mark")
    public ResponseEntity<?> mark(@RequestBody MarkAttendanceRequest req) {
        log.info("POST /api/attendance/mark called: studentUid={}, courseId={}, date={}, status={}",
                req.getStudentUid(), req.getCourseId(), req.getDate(), req.getStatus());

        String currentUid = getCurrentUid();
        if (currentUid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        String role = roleService.getRoleForUid(currentUid);
        log.info("mark(): currentUid={}, role={}", currentUid, role);

        // Only ADMIN or TEACHER can mark attendance
        if (!isPrivileged(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only ADMIN or TEACHER can mark attendance"));
        }

        try {
            attendanceService.markAttendance(
                    req.getCourseId(),   // can be null or empty → service will handle
                    req.getDate(),
                    req.getStudentUid(),
                    req.getStatus(),
                    currentUid
            );
            return ResponseEntity.ok(Map.of("message", "Attendance marked"));
        } catch (IllegalArgumentException ex) {
            log.warn("mark(): bad request: {}", ex.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("mark(): internal error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to mark attendance"));
        }
    }

    // ---------- GET: list records for student ----------

    /**
     * GET /api/attendance/student/{studentUid}
     * GET /api/attendance/student/{studentUid}?courseId=SOMETHING (optional)
     *
     * - ADMIN / TEACHER can view any student's attendance.
     * - STUDENT can view only their own attendance.
     *
     * If courseId is missing -> fetch **all courses** for that student.
     */
    @GetMapping("/student/{studentUid}")
    public ResponseEntity<?> getAttendance(
            @PathVariable String studentUid,
            @RequestParam(name = "courseId", required = false) String courseId) {

        log.info("GET /api/attendance/student/{} (courseId={})", studentUid, courseId);

        String currentUid = getCurrentUid();
        if (currentUid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        String role = roleService.getRoleForUid(currentUid);
        boolean privileged = isPrivileged(role);
        log.info("getAttendance(): currentUid={}, role={}, privileged={}", currentUid, role, privileged);

        // If not admin/teacher, must be the same student
        if (!privileged && !currentUid.equals(studentUid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Not allowed to view other students' attendance"));
        }

        try {
            List<AttendanceRecord> records =
                    attendanceService.getAttendanceForStudent(courseId, studentUid);
            return ResponseEntity.ok(records);
        } catch (IllegalArgumentException ex) {
            log.warn("getAttendance(): bad request: {}", ex.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("getAttendance(): internal error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch attendance"));
        }
    }

    // ---------- GET: summary for student ----------

    /**
     * GET /api/attendance/student/{studentUid}/summary
     * GET /api/attendance/student/{studentUid}/summary?courseId=SOMETHING (optional)
     *
     * If courseId is missing -> summary across **all courses**.
     */
    @GetMapping("/student/{studentUid}/summary")
    public ResponseEntity<?> getSummary(
            @PathVariable String studentUid,
            @RequestParam(name = "courseId", required = false) String courseId) {

        log.info("GET /api/attendance/student/{}/summary (courseId={})", studentUid, courseId);

        String currentUid = getCurrentUid();
        if (currentUid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        String role = roleService.getRoleForUid(currentUid);
        boolean privileged = isPrivileged(role);
        log.info("getSummary(): currentUid={}, role={}, privileged={}", currentUid, role, privileged);

        if (!privileged && !currentUid.equals(studentUid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Not allowed to view other students' attendance"));
        }

        try {
            AttendanceSummary summary =
                    attendanceService.getSummaryForStudent(courseId, studentUid);

            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException ex) {
            log.warn("getSummary(): bad request: {}", ex.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("getSummary(): internal error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch attendance summary"));
        }
    }
}
