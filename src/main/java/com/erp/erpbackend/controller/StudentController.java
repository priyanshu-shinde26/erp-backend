package com.erp.erpbackend.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.database.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * StudentController
 * - GET /api/students/{id}
 * - GET /api/students/me
 * - POST /api/students   (create/update student record for authenticated user)
 *
 * NOTE: There is a commented debug POST /debug/create/{id} showing how to restrict to admin role.
 * Remove or secure debug endpoints before production.
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {
    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    private final DatabaseReference studentsRef;
    private final DatabaseReference rolesRootRef;

    public StudentController() {
        this.studentsRef = FirebaseDatabase.getInstance().getReference("students");
        this.rolesRootRef = FirebaseDatabase.getInstance().getReference("roles");
        logger.info("StudentController initialized. studentsRef path: {}", studentsRef.getPath().toString());
    }

    // GET student by id (requires Authorization: Bearer <idToken>)
    @GetMapping("/{id}")
    public ResponseEntity<?> getStudentById(
            @PathVariable("id") String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        logger.info("GET /api/students/{} called", id);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing Authorization header");
        }

        String idToken = authHeader.substring(7);
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            logger.info("Token verified for uid={}", decoded.getUid());

            final CountDownLatch latch = new CountDownLatch(1);
            final Object[] holder = new Object[1];

            studentsRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    logger.info("onDataChange called for id={}, exists={}", id, snapshot.exists());
                    holder[0] = snapshot.exists() ? snapshot.getValue() : null;
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    logger.error("onCancelled called for id={}. code={}, message={}", id, error.getCode(), error.getMessage());
                    holder[0] = error;
                    latch.countDown();
                }
            });

            // Wait up to 10 seconds for DB read
            if (!latch.await(10, TimeUnit.SECONDS)) {
                logger.error("Database read timed out for id={}", id);
                return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("Database read timed out");
            }

            Object result = holder[0];
            if (result == null) {
                logger.info("Student not found: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found");
            } else if (result instanceof DatabaseError) {
                DatabaseError err = (DatabaseError) result;
                logger.error("Firebase DatabaseError for id={}: {}", id, err.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Firebase error: " + err.getMessage());
            } else {
                logger.info("Returning student data for id={}", id);
                return ResponseEntity.ok(result);
            }

        } catch (Exception e) {
            logger.error("Token verification or other error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token verification failed: " + e.getMessage());
        }
    }

    // GET /api/students/me - returns logged-in user's student record
    @GetMapping("/me")
    public ResponseEntity<?> getMyStudent(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        logger.info("GET /api/students/me called");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header on /me");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing Authorization header");
        }

        String idToken = authHeader.substring(7);
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decoded.getUid();
            logger.info("Verified token for uid={}", uid);

            final CountDownLatch latch = new CountDownLatch(1);
            final Object[] holder = new Object[1];

            studentsRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    logger.info("onDataChange(/me) exists={}", snapshot.exists());
                    holder[0] = snapshot.exists() ? snapshot.getValue() : null;
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    logger.error("onCancelled(/me): {}", error.getMessage());
                    holder[0] = error;
                    latch.countDown();
                }
            });

            if (!latch.await(10, TimeUnit.SECONDS)) {
                logger.error("Database read timed out for /me");
                return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("Database read timed out");
            }

            Object result = holder[0];
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found");
            } else if (result instanceof DatabaseError) {
                DatabaseError err = (DatabaseError) result;
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Firebase error: " + err.getMessage());
            } else {
                return ResponseEntity.ok(result);
            }

        } catch (Exception e) {
            logger.error("Token verification failed for /me: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token verification failed: " + e.getMessage());
        }
    }

    // POST /api/students - create or update the calling user's student record
    @PostMapping
    public ResponseEntity<?> createOrUpdateStudent(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> payload) {

        logger.info("POST /api/students called");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header on POST");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing Authorization header");
        }

        String idToken = authHeader.substring(7);
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decoded.getUid();
            logger.info("Verified token for uid={}", uid);

            // Ensure backend uses authenticated UID as key
            payload.put("uid", uid);

            // Save synchronously for simple request/response flow
            studentsRef.child(uid).setValueAsync(payload).get();

            logger.info("Saved/updated student record for uid={}", uid);
            return ResponseEntity.status(HttpStatus.CREATED).body(payload);

        } catch (Exception e) {
            logger.error("Failed to create/update student: {}", e.getMessage(), e);
            // If token verification failed this will be the cause -> return 401
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("token")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token verification failed: " + e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save student: " + e.getMessage());
        }
    }

    /*
     * DEBUG: create a student for {id} only if the requester is admin.
     * This method is commented out by default. If you need a debug-create endpoint,
     * uncomment and use it temporarily. It demonstrates reading /roles/{uid}
     * using an async listener + CountDownLatch instead of rolesRef.get().
     *
    @PostMapping("/debug/create/{id}")
    public ResponseEntity<?> createDebugStudent(@PathVariable String id,
                                                @RequestHeader(value = "Authorization", required = false) String authHeader,
                                                @RequestBody Map<String, Object> payload) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing Authorization header");
        }
        try {
            String idToken = authHeader.substring(7);
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String requesterUid = decoded.getUid();

            // Read role for requesterUid using listener + latch (no rolesRef.get() usage)
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicBoolean isAdmin = new AtomicBoolean(false);

            rolesRootRef.child(requesterUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String role = String.valueOf(snapshot.getValue());
                        if ("admin".equals(role)) isAdmin.set(true);
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    latch.countDown();
                }
            });

            if (!latch.await(5, TimeUnit.SECONDS)) {
                return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("Roles read timed out");
            }

            if (!isAdmin.get()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin only");
            }

            // perform debug create for the specified id
            payload.put("uid", id);
            studentsRef.child(id).setValueAsync(payload).get();
            return ResponseEntity.ok(payload);

        } catch (Exception e) {
            logger.error("createDebugStudent failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("createDebugStudent failed: " + e.getMessage());
        }
    }
    */

}
