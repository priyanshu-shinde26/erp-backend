package com.erp.erpbackend.attendance;

import com.google.firebase.database.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final DatabaseReference studentsRef =
            FirebaseDatabase.getInstance().getReference("students");

    /**
     * Derives UNIQUE class list from students.classId
     */
    @GetMapping
    public List<ClassModel> getAllClasses() throws InterruptedException {

        Set<String> classIds = new HashSet<>();
        List<ClassModel> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot s : snapshot.getChildren()) {
                    String classId = s.child("classId").getValue(String.class);
                    if (classId != null && !classIds.contains(classId)) {
                        classIds.add(classId);

                        ClassModel c = new ClassModel();
                        c.classId = classId;
                        c.name = classId;
                        c.course = "";
                        c.year = "";
                        result.add(c);
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
        return result;
    }
}
