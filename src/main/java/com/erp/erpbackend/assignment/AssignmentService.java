package com.erp.erpbackend.assignment;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.erp.erpbackend.model.Student;
import com.erp.erpbackend.service.RoleService;
import com.google.firebase.database.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final FirebaseDatabase firebaseDatabase;
    private final RoleService roleService;
    private final Cloudinary cloudinary;

    public AssignmentService(FirebaseDatabase firebaseDatabase,
                             RoleService roleService,
                             Cloudinary cloudinary) {
        this.firebaseDatabase = firebaseDatabase;
        this.roleService = roleService;
        this.cloudinary = cloudinary;
    }

    private DatabaseReference assignmentsRef() {
        return firebaseDatabase.getReference("assignments");
    }

    private DatabaseReference submissionsRef() {
        return firebaseDatabase.getReference("assignmentSubmissions");
    }

    // =============== ASSIGNMENT CRUD ===============

    public Assignment createAssignment(AssignmentDtos.CreateAssignmentRequest request,
                                       String creatorUid) {

        if (!isTeacherOrAdmin(creatorUid)) {
            throw new AccessDeniedException("Only teacher or admin can create assignments");
        }

        String id = assignmentsRef().push().getKey();
        if (id == null) {
            throw new RuntimeException("Failed to generate assignment id");
        }

        long now = Instant.now().toEpochMilli();

        if (request.getClassId() == null || request.getClassId().isBlank()) {
            throw new IllegalArgumentException("classId is required");
        }

        Assignment assignment = new Assignment(
                id,
                request.getTitle(),
                request.getDescription(),
                request.getClassId(),
                request.getSubject(),
                now,
                request.getDueDate(),
                creatorUid,
                true
        );

        assignmentsRef().child(id).setValueAsync(assignment);
        return assignment;
    }

    public Assignment updateAssignment(String assignmentId,
                                       AssignmentDtos.UpdateAssignmentRequest request,
                                       String editorUid) {

        if (!isTeacherOrAdmin(editorUid)) {
            throw new AccessDeniedException("Only teacher or admin can update assignments");
        }

        Assignment existing = getAssignment(assignmentId);

        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());

        if (request.getClassId() != null && !request.getClassId().isBlank()) {
            existing.setClassId(request.getClassId());
        }

        existing.setSubject(request.getSubject());
        existing.setDueDate(request.getDueDate());

        assignmentsRef().child(assignmentId).setValueAsync(existing);
        return existing;
    }

    /**
     * Teacher/Admin uploads or replaces the question PDF.
     * REAL implementation: upload to Cloudinary and store HTTPS URL.
     */
    public Assignment uploadQuestionFile(String assignmentId,
                                         String uploaderUid,
                                         MultipartFile file) {

        if (!isTeacherOrAdmin(uploaderUid)) {
            throw new AccessDeniedException("Only teacher or admin can upload question files");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Question file is empty");
        }

        Assignment assignment = getAssignment(assignmentId);
        if (assignment == null || !assignment.isActive()) {
            throw new RuntimeException("Assignment not found or inactive");
        }

        try {
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                originalName = "question.pdf";
            }

            String publicId = "assignments/questions/" + assignmentId + "/" + originalName;

            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "raw"
                    )
            );

            String secureUrl = (String) uploadResult.get("secure_url");
            if (secureUrl == null) {
                secureUrl = (String) uploadResult.get("url");
            }
            if (secureUrl == null) {
                throw new RuntimeException("Cloudinary did not return a URL");
            }

            assignment.setQuestionFileUrl(secureUrl);
            assignment.setQuestionFilePublicId(publicId);

            assignmentsRef().child(assignmentId).setValueAsync(assignment);
            return assignment;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload question PDF to Cloudinary", e);
        }
    }

    public void deleteAssignment(String assignmentId, String editorUid) {
        if (!isTeacherOrAdmin(editorUid)) {
            throw new AccessDeniedException("Only teacher or admin can delete assignments");
        }

        Assignment existing = getAssignment(assignmentId);
        existing.setActive(false);
        assignmentsRef().child(assignmentId).setValueAsync(existing);
    }

    public void deleteAssignmentsBulk(List<String> ids, String editorUid) {
        if (!isTeacherOrAdmin(editorUid)) {
            throw new AccessDeniedException("Only teacher/admin can bulk delete assignments");
        }
        if (ids == null || ids.isEmpty()) return;

        for (String id : ids) {
            try {
                deleteAssignment(id, editorUid);
            } catch (Exception e) {
                System.err.println("Failed to delete assignment " + id + ": " + e.getMessage());
            }
        }
    }

    // 🔥 FIXED: ADMIN gets ALL assignments from Firebase (same as getAllAssignments())
    public List<Assignment> getAllAssignmentsAdmin(String adminUid) {
        if (!roleService.hasRole(adminUid, "ADMIN")) {
            throw new AccessDeniedException("Only admin can access all assignments");
        }
        // 🔥 Returns ALL active assignments (same logic as getAllAssignments())
        CompletableFuture<List<Assignment>> future = new CompletableFuture<>();

        assignmentsRef()
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<Assignment> result = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Assignment a = child.getValue(Assignment.class);
                            if (a != null && a.isActive()) {
                                result.add(a);
                            }
                        }
                        future.complete(result);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        future.completeExceptionally(error.toException());
                    }
                });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to load assignments", e);
        }
    }

    public List<Assignment> getAllAssignments() {
        CompletableFuture<List<Assignment>> future = new CompletableFuture<>();

        assignmentsRef()
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<Assignment> result = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Assignment a = child.getValue(Assignment.class);
                            if (a != null && a.isActive()) {
                                result.add(a);
                            }
                        }
                        future.complete(result);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        future.completeExceptionally(error.toException());
                    }
                });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to load assignments", e);
        }
    }

    public List<Assignment> getAssignmentsForClass(String classId) {

        if (classId == null || classId.isBlank()) {
            throw new IllegalArgumentException("classId is required");
        }

        CompletableFuture<List<Assignment>> future = new CompletableFuture<>();

        assignmentsRef()
                .orderByChild("classId")
                .equalTo(classId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<Assignment> result = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Assignment a = child.getValue(Assignment.class);
                            if (a != null && a.isActive()) {
                                result.add(a);
                            }
                        }
                        future.complete(result);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        future.completeExceptionally(error.toException());
                    }
                });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to load assignments", e);
        }
    }

    public Assignment getAssignment(String assignmentId) {
        CompletableFuture<Assignment> future = new CompletableFuture<>();

        assignmentsRef().child(assignmentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Assignment a = snapshot.getValue(Assignment.class);
                        future.complete(a);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        future.completeExceptionally(error.toException());
                    }
                });

        try {
            Assignment assignment = future.get();
            if (assignment == null) {
                throw new RuntimeException("Assignment not found");
            }
            return assignment;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to get assignment", e);
        }
    }

    // =============== STUDENT SUBMISSIONS ===============

    public AssignmentSubmission submitAssignment(String assignmentId,
                                                 String studentUid,
                                                 MultipartFile file) {

        Assignment assignment = getAssignment(assignmentId);
        long now = Instant.now().toEpochMilli();

        if (now > assignment.getDueDate()) {
            throw new IllegalStateException("Submission deadline has passed");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Submission file is empty");
        }

        String submissionId = submissionsRef()
                .child(assignmentId)
                .child(studentUid)
                .push()
                .getKey();

        if (submissionId == null) {
            throw new RuntimeException("Failed to generate submission id");
        }

        try {
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                originalName = "submission.pdf";
            }

            String publicId = "assignments/submissions/" + assignmentId
                    + "/" + studentUid
                    + "/" + submissionId
                    + "/" + originalName;

            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "raw"
                    )
            );

            String secureUrl = (String) uploadResult.get("secure_url");
            if (secureUrl == null) {
                secureUrl = (String) uploadResult.get("url");
            }
            if (secureUrl == null) {
                throw new RuntimeException("Cloudinary did not return a URL");
            }

            AssignmentSubmission submission = new AssignmentSubmission(
                    submissionId,
                    assignmentId,
                    studentUid,
                    now,
                    secureUrl,
                    publicId,
                    false,
                    null,
                    null,
                    null,
                    null
            );

            submissionsRef()
                    .child(assignmentId)
                    .child(studentUid)
                    .child(submissionId)
                    .setValueAsync(submission);

            return submission;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload submission to Cloudinary", e);
        }
    }

    public void deleteOwnSubmission(String assignmentId,
                                    String studentUid,
                                    String submissionId) {

        Assignment assignment = getAssignment(assignmentId);
        long now = Instant.now().toEpochMilli();

        if (now > assignment.getDueDate()) {
            throw new IllegalStateException("Cannot delete after due date");
        }

        AssignmentSubmission existing = getSubmission(assignmentId, studentUid, submissionId);
        if (existing == null || existing.isDeleted()) {
            throw new RuntimeException("Submission not found or already deleted");
        }

        existing.setDeleted(true);
        submissionsRef()
                .child(assignmentId)
                .child(studentUid)
                .child(submissionId)
                .setValueAsync(existing);
    }

    public AssignmentSubmission getSubmission(String assignmentId,
                                              String studentUid,
                                              String submissionId) {

        CompletableFuture<AssignmentSubmission> future = new CompletableFuture<>();

        submissionsRef()
                .child(assignmentId)
                .child(studentUid)
                .child(submissionId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        AssignmentSubmission s = snapshot.getValue(AssignmentSubmission.class);
                        future.complete(s);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        future.completeExceptionally(error.toException());
                    }
                });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to get submission", e);
        }
    }

    public List<AssignmentSubmission> getSubmissionsForAssignment(String assignmentId,
                                                                  String requesterUid) {

        if (!isTeacherOrAdmin(requesterUid)) {
            throw new AccessDeniedException("Only teacher/admin can view all submissions");
        }

        CompletableFuture<List<AssignmentSubmission>> future = new CompletableFuture<>();

        submissionsRef()
                .child(assignmentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<AssignmentSubmission> result = new ArrayList<>();
                        for (DataSnapshot studentNode : snapshot.getChildren()) {
                            for (DataSnapshot subNode : studentNode.getChildren()) {
                                AssignmentSubmission s = subNode.getValue(AssignmentSubmission.class);
                                if (s != null && !s.isDeleted()) {
                                    result.add(s);
                                }
                            }
                        }
                        future.complete(result);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        future.completeExceptionally(error.toException());
                    }
                });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to load submissions", e);
        }
    }

    public AssignmentSubmission gradeSubmission(String assignmentId,
                                                String studentUid,
                                                String submissionId,
                                                AssignmentDtos.GradeSubmissionRequest req,
                                                String graderUid) {

        if (!isTeacherOrAdmin(graderUid)) {
            throw new AccessDeniedException("Only teacher/admin can grade submissions");
        }

        AssignmentSubmission submission = getSubmission(assignmentId, studentUid, submissionId);
        if (submission == null || submission.isDeleted()) {
            throw new RuntimeException("Submission not found");
        }

        submission.setMarks(req.getMarks());
        submission.setFeedback(req.getFeedback());
        submission.setGradedByUid(graderUid);
        submission.setGradedAt(Instant.now().toEpochMilli());

        submissionsRef()
                .child(assignmentId)
                .child(studentUid)
                .child(submissionId)
                .setValueAsync(submission);

        return submission;
    }

    // =============== STATUS / OVERVIEW ===============

    public AssignmentStatusDto getAssignmentStatus(String assignmentId, String requesterUid) {

        if (!isTeacherOrAdmin(requesterUid)) {
            throw new AccessDeniedException("Only teacher/admin can view assignment status");
        }

        Assignment assignment = getAssignment(assignmentId);

        CompletableFuture<AssignmentStatusDto> future = new CompletableFuture<>();

        submissionsRef()
                .child(assignmentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        int total = 0;
                        int graded = 0;
                        Set<String> distinctStudents = new HashSet<>();

                        for (DataSnapshot studentNode : snapshot.getChildren()) {
                            for (DataSnapshot subNode : studentNode.getChildren()) {
                                AssignmentSubmission s = subNode.getValue(AssignmentSubmission.class);
                                if (s != null && !s.isDeleted()) {
                                    total++;
                                    distinctStudents.add(s.getStudentUid());
                                    if (s.getMarks() != null) {
                                        graded++;
                                    }
                                }
                            }
                        }

                        AssignmentStatusDto dto = new AssignmentStatusDto();
                        dto.setAssignmentId(assignment.getId());
                        dto.setTitle(assignment.getTitle());
                        dto.setClassId(assignment.getClassId());
                        dto.setDueDate(assignment.getDueDate());
                        dto.setTotalSubmissions(total);
                        dto.setDistinctStudentsSubmitted(distinctStudents.size());
                        dto.setGradedCount(graded);
                        dto.setUngradedCount(total - graded);

                        future.complete(dto);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        future.completeExceptionally(error.toException());
                    }
                });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to compute assignment status", e);
        }
    }

    // =============== ROLE HELPER ===============

    private boolean isTeacherOrAdmin(String uid) {
        return roleService.hasRole(uid, "TEACHER")
                || roleService.hasRole(uid, "ADMIN");
    }

    // =============== 🔥 NEW: GET STUDENT CLASS & GRADES ===============

    /**
     * 🔥 Get student's classId from Firebase
     */
    public String getStudentClassId(String studentUid) {
        CompletableFuture<String> future = new CompletableFuture<>();

        firebaseDatabase.getReference("students")  // Change to "users" if needed
                .child(studentUid)
                .child("classId")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String classId = snapshot.getValue(String.class);
                        future.complete(classId);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        future.complete(null);
                    }
                });

        try {
            return future.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to get classId for student: " + studentUid);
            return null;
        }
    }

    /**
     * 🔥 Get student's submission + grade for specific assignment
     */
    public AssignmentSubmission findSubmissionByStudentAndAssignment(String studentUid, String assignmentId) {
        CompletableFuture<AssignmentSubmission> future = new CompletableFuture<>();

        submissionsRef()
                .child(assignmentId)
                .child(studentUid)
                .limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.hasChildren()) {
                            DataSnapshot firstChild = snapshot.getChildren().iterator().next();
                            AssignmentSubmission submission = firstChild.getValue(AssignmentSubmission.class);
                            future.complete(submission);
                        } else {
                            future.complete(null);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        future.complete(null);
                    }
                });

        try {
            return future.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("No submission found for {} on assignment {}", studentUid, assignmentId);
            return null;
        }
    }

    /**
     * 🔥 Check if user is student (for controller logic)
     */
    public boolean isStudent(String uid) {
        return !isTeacherOrAdmin(uid);
    }
}
