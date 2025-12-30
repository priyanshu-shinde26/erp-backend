package com.erp.erpbackend.assignment;

import com.erp.erpbackend.assignment.AssignmentDtos.CreateAssignmentRequest;
import com.erp.erpbackend.assignment.AssignmentDtos.UpdateAssignmentRequest;
import com.erp.erpbackend.assignment.AssignmentDtos.GradeSubmissionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assignments")
@CrossOrigin
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    private String currentUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new RuntimeException("Unauthenticated");
        }
        return auth.getName();
    }

    // 🔥 NEW: ADMIN ONLY - ALL assignments (NO classId required)
    @GetMapping("/admin")
    public ResponseEntity<List<Assignment>> getAllAssignmentsAdmin() {
        String uid = currentUid();
        // 🔥 ADMIN gets ALL assignments across ALL classes
        List<Assignment> allAssignments = assignmentService.getAllAssignmentsAdmin(uid);
        return ResponseEntity.ok(allAssignments);
    }

    // ================= TEACHER / ADMIN =================

    // ✅ CREATE ASSIGNMENT (classId REQUIRED)
    @PostMapping
    public ResponseEntity<?> createAssignment(@RequestBody CreateAssignmentRequest request) {
        if (request.getClassId() == null || request.getClassId().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "classId is required"));
        }

        String uid = currentUid();
        Assignment created = assignmentService.createAssignment(request, uid);
        return ResponseEntity.ok(created);
    }

    // UPDATE ASSIGNMENT
    @PutMapping("/{assignmentId}")
    public ResponseEntity<Assignment> updateAssignment(
            @PathVariable String assignmentId,
            @RequestBody UpdateAssignmentRequest request) {

        String uid = currentUid();
        Assignment updated = assignmentService.updateAssignment(assignmentId, request, uid);
        return ResponseEntity.ok(updated);
    }

    // UPLOAD / REPLACE QUESTION PDF
    @PostMapping("/question/upload")
    public ResponseEntity<Assignment> uploadQuestionFile(
            @RequestParam("assignmentId") String assignmentId,
            @RequestParam("file") MultipartFile file) throws IOException {

        String uid = currentUid();
        Assignment updated = assignmentService.uploadQuestionFile(assignmentId, uid, file);
        return ResponseEntity.ok(updated);
    }

    // DELETE SINGLE ASSIGNMENT
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable String assignmentId) {
        String uid = currentUid();
        assignmentService.deleteAssignment(assignmentId, uid);
        return ResponseEntity.noContent().build();
    }

    // BULK DELETE
    @DeleteMapping
    public ResponseEntity<Void> deleteAssignmentsBulk(@RequestParam("ids") List<String> ids) {
        String uid = currentUid();
        assignmentService.deleteAssignmentsBulk(ids, uid);
        return ResponseEntity.noContent().build();
    }

    // ================= FIXED: CLASS-WISE FETCH + STUDENT GRADES =================

    // 🔥 STUDENTS: Get their class assignments + THEIR grades
    // 🔥 TEACHERS: Get specific class assignments
    @GetMapping
    public ResponseEntity<?> getAssignments(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String classId) {

        try {
            // 🔥 Extract UID from token (works for both students + teachers)
            String uid = extractUidFromToken(authHeader);

            // 🔥 For students: get their classId from Firebase
            // 🔥 For teachers: use provided classId param
            String finalClassId;
            if (assignmentService.isStudent(uid)) {
                // Student: use their actual classId
                finalClassId = assignmentService.getStudentClassId(uid);
                if (finalClassId == null || finalClassId.isBlank()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Student classId not found"));
                }
            } else {
                // Teacher: use provided classId
                finalClassId = classId;
                if (finalClassId == null || finalClassId.isBlank()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "classId is required for teachers"));
                }
            }

            // 🔥 Get assignments for the class
            List<Assignment> assignments = assignmentService.getAssignmentsForClass(finalClassId);

            // 🔥 Add grades ONLY for students (their own submissions)
            List<Assignment> result = assignments.stream().map(assignment -> {
                Assignment item = assignment.clone(); // Copy assignment

                if (assignmentService.isStudent(uid)) {
                    // 🔥 Add THIS student's grade
                    AssignmentSubmission submission = assignmentService
                            .findSubmissionByStudentAndAssignment(uid, assignment.getId());

                    if (submission != null) {
                        item.setMarks(submission.getMarks());
                        item.setFeedback(submission.getFeedback());
                    }
                }

                return item;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    // 🔥 Extract UID from Firebase token (simple version)
    private String extractUidFromToken(String authHeader) {
        try {
            String idToken = authHeader.replace("Bearer ", "");
            // Use your existing currentUid() for Spring Security
            return currentUid();
        } catch (Exception e) {
            throw new RuntimeException("Invalid token");
        }
    }

    // GET SINGLE ASSIGNMENT
    @GetMapping("/{assignmentId}")
    public ResponseEntity<Assignment> getAssignment(@PathVariable String assignmentId) {
        Assignment assignment = assignmentService.getAssignment(assignmentId);
        return ResponseEntity.ok(assignment);
    }

    // ================= STUDENT =================

    // SUBMIT ASSIGNMENT
    @PostMapping("/submissions/upload")
    public ResponseEntity<?> submitAssignment(
            @RequestParam("assignmentId") String assignmentId,
            @RequestParam("file") MultipartFile file) throws IOException {

        String uid = currentUid();
        Assignment assignment = assignmentService.getAssignment(assignmentId);

        if (!assignment.isSubmissionAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Assignment is closed. Submission not allowed."));
        }

        AssignmentSubmission submission =
                assignmentService.submitAssignment(assignmentId, uid, file);

        return ResponseEntity.ok(submission);
    }

    // DELETE OWN SUBMISSION
    @DeleteMapping("/{assignmentId}/submissions/{submissionId}")
    public ResponseEntity<Void> deleteOwnSubmission(
            @PathVariable String assignmentId,
            @PathVariable String submissionId) {

        String uid = currentUid();
        assignmentService.deleteOwnSubmission(assignmentId, uid, submissionId);
        return ResponseEntity.noContent().build();
    }

    // ================= TEACHER =================

    // GET ALL SUBMISSIONS
    @GetMapping("/{assignmentId}/submissions")
    public ResponseEntity<List<AssignmentSubmission>> getSubmissionsForAssignment(
            @PathVariable String assignmentId) {

        String uid = currentUid();
        List<AssignmentSubmission> submissions =
                assignmentService.getSubmissionsForAssignment(assignmentId, uid);
        return ResponseEntity.ok(submissions);
    }

    // GRADE SUBMISSION
    @PostMapping("/{assignmentId}/submissions/{studentUid}/{submissionId}/grade")
    public ResponseEntity<AssignmentSubmission> gradeSubmission(
            @PathVariable String assignmentId,
            @PathVariable String studentUid,
            @PathVariable String submissionId,
            @RequestBody GradeSubmissionRequest request) {

        String graderUid = currentUid();
        AssignmentSubmission graded = assignmentService.gradeSubmission(
                assignmentId, studentUid, submissionId, request, graderUid
        );
        return ResponseEntity.ok(graded);
    }

    // ASSIGNMENT STATUS
    @GetMapping("/{assignmentId}/status")
    public ResponseEntity<AssignmentStatusDto> getAssignmentStatus(
            @PathVariable String assignmentId) {

        String uid = currentUid();
        AssignmentStatusDto status = assignmentService.getAssignmentStatus(assignmentId, uid);
        return ResponseEntity.ok(status);
    }
}
