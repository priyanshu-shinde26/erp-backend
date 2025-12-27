package com.erp.erpbackend.attendance;

import com.google.firebase.database.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class StudentService {

    private final DatabaseReference studentRef =
            FirebaseDatabase.getInstance().getReference("students");

    public List<StudentDto> getAllStudents() {

        List<StudentDto> list = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot s : snapshot.getChildren()) {

                    String roll = s.child("rollNumber").getValue(String.class);
                    String name = s.child("name").getValue(String.class);

                    if (roll != null && name != null) {
                        list.add(new StudentDto(roll, name));
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
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return list;
    }
}
