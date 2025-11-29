package com.erp.erpbackend.service;

import com.google.firebase.database.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class RoleService {

    private final DatabaseReference rolesRef;

    public RoleService(@Qualifier("rolesRef") DatabaseReference rolesRef) {
        this.rolesRef = rolesRef;
        System.out.println("RoleService: rolesRef path = " + rolesRef.getPath().toString());
    }

    public String getRoleForUid(String uid) {
        if (uid == null || uid.isBlank()) {
            return "STUDENT";
        }

        AtomicReference<String> roleRef = new AtomicReference<>("STUDENT");
        CountDownLatch latch = new CountDownLatch(1);

        rolesRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String value = snapshot.getValue(String.class);
                    if (value != null && !value.isBlank()) {
                        roleRef.set(value.toUpperCase());
                    }
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("RoleService.getRoleForUid cancelled: " + error);
                latch.countDown();
            }
        });

        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String finalRole = roleRef.get();
        System.out.println("RoleService.getRoleForUid(" + uid + ") -> " + finalRole);
        return finalRole;
    }

    public void ensureDefaultRoleIfMissing(String uid, String defaultRole) {
        String roleToSet = (defaultRole == null || defaultRole.isBlank())
                ? "STUDENT"
                : defaultRole.toUpperCase();

        rolesRef.child(uid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData currentData) {
                if (currentData.getValue() == null) {
                    currentData.setValue(roleToSet);
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (error != null) {
                    System.err.println("ensureDefaultRoleIfMissing error: " + error);
                }
            }
        });
    }

    public void setRole(String uid, String role) {
        if (uid == null || uid.isBlank() || role == null || role.isBlank()) return;
        String normalized = role.toUpperCase();
        System.out.println("RoleService.setRole(" + uid + ", " + normalized + ")");
        rolesRef.child(uid).setValueAsync(normalized);
    }

    // NEW: helper for security
    public boolean hasRole(String uid, String role) {
        String actual = getRoleForUid(uid);
        return actual.equalsIgnoreCase(role);
    }
}
