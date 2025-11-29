package com.erp.erpbackend.assignment;

import com.erp.erpbackend.assignment.AssignmentDtos.CreateAssignmentRequest;
import com.erp.erpbackend.assignment.AssignmentDtos.UpdateAssignmentRequest;
import com.erp.erpbackend.assignment.AssignmentDtos.GradeSubmissionRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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

    // TEACHER / ADMIN: create assignment
    @PostMapping
    public ResponseEntity<Assignment> createAssignment(@RequestBody CreateAssignmentRequest request) {
        String uid = currentUid();
        Assignment created = assignmentService.createAssignment(request, uid);
        return ResponseEntity.ok(created);
    }

    // TEACHER / ADMIN: update assignment
    @PutMapping("/{assignmentId}")
    public ResponseEntity<Assignment> updateAssignment(
            @PathVariable String assignmentId,
            @RequestBody UpdateAssignmentRequest request) {

        String uid = currentUid();
        Assignment updated = assignmentService.updateAssignment(assignmentId, request, uid);
        return ResponseEntity.ok(updated);
    }

    // TEACHER / ADMIN: upload or replace question PDF
    @PostMapping("/question/upload")
    public ResponseEntity<Assignment> uploadQuestionFile(
            @RequestParam("assignmentId") String assignmentId,
            @RequestParam("file") MultipartFile file) throws IOException {

        String uid = currentUid();
        Assignment updated = assignmentService.uploadQuestionFile(assignmentId, uid, file);
        return ResponseEntity.ok(updated);
    }

    // TEACHER / ADMIN: delete assignment (single)
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable String assignmentId) {
        String uid = currentUid();
        assignmentService.deleteAssignment(assignmentId, uid);
        return ResponseEntity.noContent().build();
    }

    // TEACHER / ADMIN: bulk delete ?ids=a&ids=b&ids=c
    @DeleteMapping
    public ResponseEntity<Void> deleteAssignmentsBulk(@RequestParam("ids") List<String> ids) {
        String uid = currentUid();
        assignmentService.deleteAssignmentsBulk(ids, uid);
        return ResponseEntity.noContent().build();
    }

    // ANY AUTH: list assignments
    // - if classId provided -> only that class
    // - if no classId -> all assignments
    @GetMapping
    public ResponseEntity<List<Assignment>> getAssignments(
            @RequestParam(value = "classId", required = false) String classId) {

        List<Assignment> list;
        if (classId == null || classId.isBlank()) {
            list = assignmentService.getAllAssignments();
        } else {
            list = assignmentService.getAssignmentsForClass(classId);
        }
        return ResponseEntity.ok(list);
    }

    // ANY AUTH: get one assignment
    @GetMapping("/{assignmentId}")
    public ResponseEntity<Assignment> getAssignment(@PathVariable String assignmentId) {
        Assignment assignment = assignmentService.getAssignment(assignmentId);
        return ResponseEntity.ok(assignment);
    }

    // STUDENT: submit assignment PDF
    @PostMapping("/submissions/upload")
    public ResponseEntity<AssignmentSubmission> submitAssignment(
            @RequestParam("assignmentId") String assignmentId,
            @RequestParam("file") MultipartFile file) throws IOException {

        String uid = currentUid();
        AssignmentSubmission submission = assignmentService.submitAssignment(assignmentId, uid, file);
        return ResponseEntity.ok(submission);
    }

    // STUDENT: delete own submission
    @DeleteMapping("/{assignmentId}/submissions/{submissionId}")
    public ResponseEntity<Void> deleteOwnSubmission(
            @PathVariable String assignmentId,
            @PathVariable String submissionId) {

        String uid = currentUid();
        assignmentService.deleteOwnSubmission(assignmentId, uid, submissionId);
        return ResponseEntity.noContent().build();
    }

    // TEACHER / ADMIN: get all submissions
    @GetMapping("/{assignmentId}/submissions")
    public ResponseEntity<List<AssignmentSubmission>> getSubmissionsForAssignment(
            @PathVariable String assignmentId) {

        String uid = currentUid();
        List<AssignmentSubmission> submissions =
                assignmentService.getSubmissionsForAssignment(assignmentId, uid);
        return ResponseEntity.ok(submissions);
    }

    // TEACHER / ADMIN: grade submission
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

    // TEACHER / ADMIN: assignment status overview
    @GetMapping("/{assignmentId}/status")
    public ResponseEntity<AssignmentStatusDto> getAssignmentStatus(
            @PathVariable String assignmentId) {

        String uid = currentUid();
        AssignmentStatusDto status = assignmentService.getAssignmentStatus(assignmentId, uid);
        return ResponseEntity.ok(status);
    }
}
