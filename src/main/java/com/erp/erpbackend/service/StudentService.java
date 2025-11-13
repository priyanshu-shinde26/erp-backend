package com.erp.erpbackend.service;

import com.erp.erpbackend.model.Student;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class StudentService {

    // set during init after FirebaseApp exists
    private DatabaseReference studentsRef;

    public StudentService() {
        // constructor intentionally empty
    }

    @PostConstruct
    public void init() {
        // wait for FirebaseApp to initialize (non-blocking if not available within timeout)
        final int retries = 20;    // 20 * 500ms = 10s
        final int waitMs = 500;
        boolean ok = false;
        for (int i = 0; i < retries; i++) {
            try {
                if (!com.google.firebase.FirebaseApp.getApps().isEmpty()) {
                    this.studentsRef = FirebaseDatabase.getInstance().getReference("students");
                    System.out.println("StudentService: Firebase DatabaseReference initialized.");
                    ok = true;
                    break;
                }
            } catch (Exception e) {
                // swallow, retry
            }
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!ok) {
            System.err.println("WARNING: StudentService failed to initialize Firebase DatabaseReference within timeout. studentsRef is null.");
            this.studentsRef = null;
        }
    }

    private void ensureReady() {
        if (studentsRef == null) {
            throw new IllegalStateException("Firebase not initialized (studentsRef == null)");
        }
    }

    public void createOrUpdateStudent(Student s) {
        ensureReady();
        String key = s.getUid();
        if (key == null || key.isEmpty()) {
            key = studentsRef.push().getKey();
            s.setUid(key);
        }
        studentsRef.child(key).setValueAsync(s);
    }

    public Optional<Student> getStudent(String uid) throws InterruptedException {
        ensureReady();
        final CountDownLatch latch = new CountDownLatch(1);
        final Student[] result = new Student[1];

        DatabaseReference ref = studentsRef.child(uid);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    result[0] = snapshot.getValue(Student.class);
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                latch.countDown();
            }
        });

        latch.await(10, TimeUnit.SECONDS);
        return Optional.ofNullable(result[0]);
    }

    public List<Student> listAll() throws InterruptedException {
        ensureReady();
        final CountDownLatch latch = new CountDownLatch(1);
        final List<Student> out = new ArrayList<>();

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Student s = child.getValue(Student.class);
                        if (s != null) out.add(s);
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
        return out;
    }

    public void deleteStudent(String uid) {
        ensureReady();
        studentsRef.child(uid).removeValueAsync();
    }
}
