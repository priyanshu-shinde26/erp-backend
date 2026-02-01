package com.erp.erpbackend.academic_module;

import com.google.firebase.database.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Repository
public class AcademicRepository {

    private final DatabaseReference db =
            FirebaseDatabase.getInstance().getReference();

    // ================= BASIC NODES =================

    public DatabaseReference roles() {
        return db.child("roles");
    }

    public DatabaseReference users() {
        return db.child("users");
    }

    public DatabaseReference subjects(String classId) {
        return db.child("subjects").child(classId);
    }

    public DatabaseReference tests(String classId, String subjectId) {
        return db.child("tests").child(classId).child(subjectId);
    }

    public DatabaseReference marks(String classId, String subjectId, String testId) {
        return db.child("marks").child(classId).child(subjectId).child(testId);
    }

    // =====================================================
    // ✅ FIXED: ASYNC GET TESTS (NO .get())
    // =====================================================
    public CompletableFuture<List<TestModel>> getTests(
            String classId,
            String subjectId) {

        CompletableFuture<List<TestModel>> future = new CompletableFuture<>();

        tests(classId, subjectId)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(DataSnapshot snapshot) {

                                List<TestModel> list = new ArrayList<>();

                                for (DataSnapshot s : snapshot.getChildren()) {
                                    TestModel model =
                                            s.getValue(TestModel.class);
                                    if (model != null) {
                                        model.testId = s.getKey();
                                        list.add(model);
                                    }
                                }

                                future.complete(list);
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                future.completeExceptionally(
                                        new RuntimeException(error.getMessage())
                                );
                            }
                        }
                );

        return future;
    }
}
