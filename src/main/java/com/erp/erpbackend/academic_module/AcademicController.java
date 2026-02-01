package com.erp.erpbackend.academic_module;

import com.erp.erpbackend.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/academic")
public class AcademicController {

    private final AcademicRepository repo;
    private final AcademicService academicService;
    private final RoleService roleService;

    public AcademicController(
           AcademicRepository repo,
            AcademicService academicService,
            RoleService roleService
    ) {
        this.repo = repo;
        this.academicService = academicService;
        this.roleService = roleService;
    }
    // =====================================================
    // GET TESTS BY CLASS + SUBJECT  (🔥 THIS WAS MISSING)
    // =====================================================

        @GetMapping("/tests")
        public List<TestModel> getTests(
                @RequestParam String classId,
                @RequestParam String subjectId
        ) {
            System.out.println("🔥 TESTS API HIT");
            return List.of(); // empty list is fine
        }


    // =====================================================
    // ✅ CREATE TEST (ADMIN / TEACHER)
    // =====================================================
    @PostMapping("/test/create")
    public ResponseEntity<?> createTest(
            HttpServletRequest request,
            @RequestParam String classId,
            @RequestParam String subjectId,
            @RequestBody TestModel model
    ) {
        String uid = (String) request.getAttribute("uid");

        System.out.println("CONTROLLER HIT uid=" + uid);

        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String role = roleService.getRoleForUid(uid);
        if (!role.equals("ADMIN") && !role.equals("TEACHER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        academicService.createTest(classId, subjectId, model, uid);
        return ResponseEntity.ok().build();
    }


    // =====================================================
    // ✅ ENTER / UPDATE MARKS (ADMIN / TEACHER)
    // =====================================================
    @PostMapping("/marks/save")
    public ResponseEntity<?> saveMarks(
            HttpServletRequest request,
            @RequestParam String classId,
            @RequestParam String subjectId,
            @RequestParam String testId,
            @RequestBody Map<String, MarksModel> marksMap
    ) {

        String uid = (String) request.getAttribute("uid");
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String role = roleService.getRoleForUid(uid);
        if (!"ADMIN".equals(role) && !"TEACHER".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        DatabaseReference ref =
                repo.marks(classId, subjectId, testId);

        for (Map.Entry<String, MarksModel> entry : marksMap.entrySet()) {

            String studentId = entry.getKey();
            MarksModel m = entry.getValue();

            m.updatedBy = uid;
            m.updatedAt = System.currentTimeMillis();

            ref.child(studentId).setValueAsync(m);
        }

        return ResponseEntity.ok("MARKS_SAVED");
    }

    // =====================================================
    // ✅ STUDENT RESULT (SELF ONLY)
    // =====================================================
    @GetMapping("/result/student")
    public ResponseEntity<?> getStudentResult(
            HttpServletRequest request,
            @RequestParam String classId
    ) throws InterruptedException {

        String uid = (String) request.getAttribute("uid");
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("marks")
                        .child(classId);

        final Object[] resultHolder = new Object[1];
        final Object lock = new Object();

        ref.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                resultHolder[0] = snapshot.child(uid).getValue();
                synchronized (lock) {
                    lock.notify();
                }
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                synchronized (lock) {
                    lock.notify();
                }
            }
        });

        synchronized (lock) {
            lock.wait();
        }

        return ResponseEntity.ok(resultHolder[0]);
    }
}
