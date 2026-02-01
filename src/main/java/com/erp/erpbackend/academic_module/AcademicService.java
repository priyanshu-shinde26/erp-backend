package com.erp.erpbackend.academic_module;

import com.erp.erpbackend.service.RoleService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
public class AcademicService {

    private final AcademicRepository repo;
    private final RoleService roleService;

    public AcademicService(
            AcademicRepository repo,
            RoleService roleService
    ) {
        this.repo = repo;
        this.roleService = roleService;
    }

    // =====================================================
    // ROLE
    // =====================================================
    public String getUserRole(String uid) {
        return roleService.getRoleForUid(uid);
    }

    // =====================================================
    // CHECK TEACHER CLASS (ASYNC)
    // =====================================================
    public void isTeacherOfClass(
            String uid,
            String classId,
            Consumer<Boolean> callback
    ) {

        DatabaseReference ref = repo.users()
                .child(uid)
                .child("assignedClasses")
                .child(classId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                callback.accept(snapshot.exists());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.accept(false);
            }
        });
    }

    // =====================================================
    // GET TESTS (ASYNC – NO .get())
    // =====================================================
    public void getTests(
            String classId,
            String subjectId,
            Consumer<List<TestModel>> callback
    ) {

        DatabaseReference ref = repo.tests(classId, subjectId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                List<TestModel> list = new ArrayList<>();

                for (DataSnapshot s : snapshot.getChildren()) {
                    TestModel model = s.getValue(TestModel.class);
                    if (model != null) {
                        model.testId = s.getKey();
                        list.add(model);
                    }
                }

                callback.accept(list);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.accept(new ArrayList<>());
            }
        });
    }

    // =====================================================
    // CREATE TEST
    // =====================================================
    public void createTest(
            String classId,
            String subjectId,
            TestModel model,
            String uid
    ) {

        model.createdBy = uid;
        model.createdAt = System.currentTimeMillis();

        repo.tests(classId, subjectId)
                .push()
                .setValueAsync(model);
    }
}
