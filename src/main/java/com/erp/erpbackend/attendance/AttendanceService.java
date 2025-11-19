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

    // ------------------- MARK ATTENDANCE --------------------

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
            throw new IllegalArgumentException("status is required (PRESENT / ABSENT)");

        // if courseId is null/blank, use a generic bucket
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

    // ------------------- GET ATTENDANCE --------------------

    /**
     * If courseId is null/blank → return records from **all courses** for this student.
     * If courseId is provided → filter only that course.
     */
    public List<AttendanceRecord> getAttendanceForStudent(String courseId,
                                                          String studentUid) {

        if (studentUid == null || studentUid.isBlank())
            throw new IllegalArgumentException("studentUid is required");

        final List<AttendanceRecord> result = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);

        if (courseId == null || courseId.isBlank()) {
            // ---- ALL COURSES for this student ----
            attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot courseSnap : snapshot.getChildren()) {
                            for (DataSnapshot dateSnap : courseSnap.getChildren()) {
                                DataSnapshot stSnap = dateSnap.child(studentUid);
                                if (stSnap.exists()) {
                                    AttendanceRecord rec = stSnap.getValue(AttendanceRecord.class);
                                    if (rec != null) {
                                        result.add(rec);
                                    }
                                }
                            }
                        }
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    System.err.println("getAttendanceForStudent(all) cancelled: " + error);
                    latch.countDown();
                }
            });
        } else {
            // ---- SINGLE COURSE ----
            DatabaseReference ref = attendanceRef.child(courseId);

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot dateSnap : snapshot.getChildren()) {
                            DataSnapshot stSnap = dateSnap.child(studentUid);
                            if (stSnap.exists()) {
                                AttendanceRecord rec = stSnap.getValue(AttendanceRecord.class);
                                if (rec != null) {
                                    result.add(rec);
                                }
                            }
                        }
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    System.err.println("getAttendanceForStudent(single) cancelled: " + error);
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        result.sort(Comparator.comparing(AttendanceRecord::getDate));
        return result;
    }

    // ------------------- GET SUMMARY --------------------

    /**
     * If courseId is null/blank → summary across all courses.
     * If courseId is provided → summary only for that course.
     */
    public AttendanceSummary getSummaryForStudent(String courseId,
                                                  String studentUid) {

        List<AttendanceRecord> records = getAttendanceForStudent(courseId, studentUid);

        int total = records.size();
        int present = 0;
        int absent = 0;

        for (AttendanceRecord r : records) {
            if ("PRESENT".equalsIgnoreCase(r.getStatus())) present++;
            if ("ABSENT".equalsIgnoreCase(r.getStatus())) absent++;
        }

        // for summary, if no specific courseId, label as "ALL"
        String summaryCourseId = (courseId == null || courseId.isBlank())
                ? "ALL"
                : courseId;

        return new AttendanceSummary(summaryCourseId, studentUid, total, present, absent);
    }
}
