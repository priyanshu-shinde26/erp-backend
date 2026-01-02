package com.erp.erpbackend.timetable;

import com.erp.erpbackend.service.RoleService;
import com.google.firebase.database.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TimetableService {

    private final FirebaseDatabase database;
    private final RoleService roleService;

    public TimetableService(FirebaseDatabase database, RoleService roleService) {
        this.database = database;
        this.roleService = roleService;
    }

    public String getCurrentUid() {
        // Extract from SecurityContextHolder / JWT token
        return "current-user-uid";  // Replace with your auth logic
    }

    // Extract unique classIds from students
    public List<String> getClasses() {
        DatabaseReference studentsRef = database.getReference("students");
        List<String> classes = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Set<String> uniqueClasses = new HashSet<>();
                for (DataSnapshot student : snapshot.getChildren()) {
                    String classId = student.child("classId").getValue(String.class);
                    if (classId != null) uniqueClasses.add(classId);
                }
                classes.addAll(uniqueClasses);
                latch.countDown();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Collections.sort(classes);
        return classes;
    }

    public void createPeriod(String classId, String day, SchedulePeriod period) {
        String periodId = database.getReference("timetables/" + classId + "/" + day).push().getKey();
        database.getReference("timetables").child(classId).child(day).child(periodId).setValueAsync(period);
    }

    public Map<String, List<SchedulePeriod>> getClassTimetable(String classId) {
        Map<String, List<SchedulePeriod>> timetable = new HashMap<>();
        String[] days = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};

        for (String day : days) {
            DatabaseReference dayRef = database.getReference("timetables").child(classId).child(day);
            List<SchedulePeriod> periods = new ArrayList<>();

            CountDownLatch latch = new CountDownLatch(1);
            dayRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    for (DataSnapshot periodSnapshot : snapshot.getChildren()) {
                        SchedulePeriod p = periodSnapshot.getValue(SchedulePeriod.class);
                        if (p != null) periods.add(p);
                    }
                    latch.countDown();
                }
                @Override
                public void onCancelled(DatabaseError error) { latch.countDown(); }
            });

            try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            if (!periods.isEmpty()) timetable.put(day, periods);
        }
        return timetable;
    }

    public List<SchedulePeriod> getDaySchedule(String classId, String day) {
        DatabaseReference dayRef = database.getReference("timetables").child(classId).child(day);
        List<SchedulePeriod> periods = new ArrayList<>();

        CountDownLatch latch = new CountDownLatch(1);
        dayRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot periodSnapshot : snapshot.getChildren()) {
                    SchedulePeriod p = periodSnapshot.getValue(SchedulePeriod.class);
                    if (p != null) periods.add(p);
                }
                latch.countDown();
            }
            @Override
            public void onCancelled(DatabaseError error) { latch.countDown(); }
        });

        try { latch.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return periods;
    }

    public void updatePeriod(String classId, String day, String periodId, SchedulePeriod period) {
        database.getReference("timetables").child(classId).child(day).child(periodId).setValueAsync(period);
    }

    public void deletePeriod(String classId, String day, String periodId) {
        database.getReference("timetables").child(classId).child(day).child(periodId).removeValueAsync();
    }
}
