package com.erp.erpbackend.LMS;

import com.cloudinary.Cloudinary;
import com.google.firebase.database.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class LmsService {

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private FirebaseDatabase firebaseDatabase;

    private static final String NOTES_PATH = "lms/notes";

    public String uploadNote(String classId, String subject, String title, MultipartFile file, String uploadedBy) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty() || !originalFilename.contains(".")) {
                throw new RuntimeException("Invalid filename: " + originalFilename);
            }

            Map<String, Object> options = new HashMap<>();
            options.put("folder", "campus-sync/lms/" + classId + "/" + (subject != null ? subject : "general"));
            options.put("resource_type", "raw");
            options.put("use_filename", true);
            options.put("unique_filename", false);
            options.put("overwrite", true);

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            String url = (String) uploadResult.get("secure_url") + "?fl_attachment&dl=1";

            DatabaseReference ref = firebaseDatabase.getReference(NOTES_PATH).child(classId).push();
            String noteId = ref.getKey();

            Note note = new Note();
            note.setId(noteId);
            note.setTitle(title);
            note.setSubject(subject != null ? subject : "");
            note.setUrl(url);
            note.setFilename(originalFilename);
            note.setTimestamp(System.currentTimeMillis());
            note.setUploadedBy(uploadedBy != null ? uploadedBy : "SYSTEM");

            // ✅ NEW: store teacher name also
            String teacherName = getUserNameFromFirebase(uploadedBy);
            if (teacherName == null || teacherName.trim().isEmpty()) {
                teacherName = uploadedBy; // fallback to uid
            }
            note.setUploadedByName(teacherName);

            ref.setValueAsync(note);
            return noteId;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Upload failed: " + e.getMessage());
        }
    }

    public void deleteNote(String classId, String noteId) {
        firebaseDatabase.getReference(NOTES_PATH)
                .child(classId)
                .child(noteId)
                .removeValueAsync();
    }

    public List<Note> getNotesForClass(String classId, String role, String userClass) {

        if ("STUDENT".equalsIgnoreCase(role)) {
            if (userClass == null || userClass.trim().isEmpty()) return new ArrayList<>();
            if (!userClass.trim().equals(classId)) return new ArrayList<>();
        }

        DatabaseReference ref = firebaseDatabase.getReference(NOTES_PATH).child(classId);

        List<Note> notes = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                try {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        try {
                            Note note = child.getValue(Note.class);
                            if (note != null) {
                                note.setId(child.getKey());

                                // ✅ PATCH: For old notes where uploadedByName is missing
                                if ((note.uploadedByName == null || note.uploadedByName.trim().isEmpty())
                                        && note.uploadedBy != null && !note.uploadedBy.trim().isEmpty()) {

                                    String teacherName = getUserNameFromFirebase(note.uploadedBy);
                                    if (teacherName != null && !teacherName.trim().isEmpty()) {
                                        note.uploadedByName = teacherName;
                                    }
                                }

                                notes.add(note);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                } finally {
                    latch.countDown();
                }
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

        return notes;
    }

    private String getUserNameFromFirebase(String uid) {
        if (uid == null || uid.trim().isEmpty()) return "SYSTEM";

        try {
            DatabaseReference userRef = firebaseDatabase.getReference("teachers").child(uid).child("name");

            CountDownLatch latch = new CountDownLatch(1);
            final String[] nameHolder = new String[1];

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    Object value = snapshot.getValue();
                    if (value != null) {
                        nameHolder[0] = value.toString();
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    latch.countDown();
                }
            });

            latch.await(3, TimeUnit.SECONDS);

            if (nameHolder[0] == null || nameHolder[0].trim().isEmpty()) return uid;
            return nameHolder[0];

        } catch (Exception e) {
            return uid;
        }
    }
}
