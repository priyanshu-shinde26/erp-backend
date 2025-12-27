package com.erp.erpbackend.attendance;

import com.google.firebase.database.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class AttendanceService {

    private final DatabaseReference attendanceRef;

    public AttendanceService() {
        this.attendanceRef = FirebaseDatabase.getInstance().getReference("attendance");
        System.out.println("AttendanceService: Firebase DatabaseReference initialized.");
    }

    // =========================================================
    // OLD METHOD (UNCHANGED – UID BASED)
    // =========================================================

    public void markAttendance(String courseId,
                               String date,
                               String studentUid,
                               String status,
                               String markedBy) {

        if (date == null || date.isBlank())
            throw new IllegalArgumentException("date is required (yyyy-MM-dd)");

        if (studentUid == null || studentUid.isBlank())
            throw new IllegalArgumentException("studentUid is required");

        if (status == null || status.isBlank())
            throw new IllegalArgumentException("status is required");

        String effectiveCourseId = (courseId == null || courseId.isBlank())
                ? "GENERAL"
                : courseId.trim();

        String normalizedStatus = status.trim().toUpperCase();
        if (!normalizedStatus.equals("PRESENT") && !normalizedStatus.equals("ABSENT"))
            throw new IllegalArgumentException("status must be PRESENT or ABSENT");

        AttendanceRecord record = new AttendanceRecord(
                effectiveCourseId,
                date,
                studentUid,
                normalizedStatus,
                markedBy
        );

        attendanceRef
                .child(effectiveCourseId)
                .child(date)
                .child(studentUid)
                .setValueAsync(record);
    }

    // =========================================================
    // NEW METHOD (ROLL NUMBER BASED – REQUIRED)
    // =========================================================

    public void markAttendanceByRollNumber(String rollNumber,
                                           String date,
                                           String status,
                                           String markedBy) {

        if (rollNumber == null || rollNumber.isBlank())
            throw new IllegalArgumentException("rollNumber is required");

        if (date == null || date.isBlank())
            throw new IllegalArgumentException("date is required");

        if (status == null || status.isBlank())
            throw new IllegalArgumentException("status is required");

        String normalizedStatus = status.trim().toUpperCase();
        if (!normalizedStatus.equals("PRESENT") && !normalizedStatus.equals("ABSENT"))
            throw new IllegalArgumentException("status must be PRESENT or ABSENT");

        // Firebase path:
        // attendance/ROLL_NUMBER/yyyy-MM-dd/{rollNumber}
        DatabaseReference ref = attendanceRef
                .child("ROLL_NUMBER")
                .child(date)
                .child(rollNumber);

        AttendanceRecord record = new AttendanceRecord();
        record.setRollNumber(rollNumber);
        record.setDate(date);
        record.setStatus(normalizedStatus);
        record.setMarkedBy(markedBy);

        ref.setValueAsync(record);
    }

    // =========================================================
    // NEW METHOD (CLASS + ROLL NUMBER BASED)
    // =========================================================

    public void markAttendanceByClass(String classId,
                                      String rollNumber,
                                      String date,
                                      String status,
                                      String markedBy) {

        if (classId == null || classId.isBlank())
            throw new IllegalArgumentException("classId is required");

        if (rollNumber == null || rollNumber.isBlank())
            throw new IllegalArgumentException("rollNumber is required");

        if (date == null || date.isBlank())
            throw new IllegalArgumentException("date is required (yyyy-MM-dd)");

        if (status == null || status.isBlank())
            throw new IllegalArgumentException("status is required");

        String normalizedStatus = status.trim().toUpperCase();
        if (!normalizedStatus.equals("PRESENT") && !normalizedStatus.equals("ABSENT"))
            throw new IllegalArgumentException("status must be PRESENT or ABSENT");

        // Firebase path: attendance/class/{classId}/{date}/{rollNumber}
        DatabaseReference ref = attendanceRef
                .child("class")
                .child(classId)
                .child(date)
                .child(rollNumber);

        AttendanceRecord record = new AttendanceRecord(
                classId,
                rollNumber,
                date,
                normalizedStatus,
                markedBy
        );

        ref.setValueAsync(record);
    }

    // =========================================================
    // GET ATTENDANCE (UID BASED – UNCHANGED)
    // =========================================================



    // =========================================================
    // SUMMARY (UID BASED – UNCHANGED)
    // =========================================================

    // ------------------- GET SUMMARY (ROLL NUMBER BASED) --------------------

    public AttendanceSummary getSummaryForStudentFromClass(
            String classId,
            String rollNumber) throws InterruptedException {

        DatabaseReference classRef =
                FirebaseDatabase.getInstance()
                        .getReference("attendance")
                        .child("class")
                        .child(classId);

        CountDownLatch latch = new CountDownLatch(1);
        int[] total = {0};
        int[] present = {0};

        classRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot classSnap) {
                for (DataSnapshot dateSnap : classSnap.getChildren()) {
                    DataSnapshot rollSnap = dateSnap.child(rollNumber);
                    if (rollSnap.exists()) {
                        total[0]++;
                        String status =
                                rollSnap.child("status").getValue(String.class);
                        if ("PRESENT".equalsIgnoreCase(status)) {
                            present[0]++;
                        }
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

        AttendanceSummary summary = new AttendanceSummary();
        summary.setTotalClasses(total[0]);
        summary.setPresentCount(present[0]);
        summary.setAbsentCount(total[0] - present[0]);
        summary.setAttendancePercentage(
                total[0] == 0 ? 0 : (present[0] * 100.0 / total[0])
        );

        return summary;
    }

    // ------------------- GET ATTENDANCE (ROLL NUMBER BASED) --------------------
    public List<AttendanceRecord> getAttendanceForStudentFromClass(
            String classId,
            String rollNumber
    ) throws InterruptedException {

        List<AttendanceRecord> list = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("attendance")
                        .child("class")
                        .child(classId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot dateSnap : snapshot.getChildren()) {
                    DataSnapshot rollSnap =
                            dateSnap.child(rollNumber);

                    if (rollSnap.exists()) {
                        AttendanceRecord r =
                                rollSnap.getValue(AttendanceRecord.class);
                        list.add(r);
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
        return list;
    }


    public List<AttendanceRecord> getAttendanceForClass(String classId, String date) {
        final List<AttendanceRecord> result = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);

        attendanceRef.child("class")
                .child(classId)
                .child(date)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot rollSnap : snapshot.getChildren()) {
                            AttendanceRecord rec = rollSnap.getValue(AttendanceRecord.class);
                            if (rec != null) {
                                result.add(rec);
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

        result.sort(Comparator.comparing(AttendanceRecord::getRollNumber));
        return result;
    }
}
