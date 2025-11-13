package com.erp.erpbackend.service;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.*;

@Service
public class RoleService {
    private final Logger log = LoggerFactory.getLogger(RoleService.class);
    private final DatabaseReference rolesRef;
    private final ConcurrentHashMap<String, CachedRole> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
    private static final long CACHE_TTL_SECONDS = 60L;

    public RoleService(@Qualifier("rolesRef") DatabaseReference rolesRef) {
        this.rolesRef = rolesRef;
        cleaner.scheduleAtFixedRate(this::cleanup, CACHE_TTL_SECONDS, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    public String getRoleForUid(String uid) {
        if (uid == null) return null;
        CachedRole cached = cache.get(uid);
        if (cached != null && !cached.isExpired()) return cached.role;

        final CountDownLatch latch = new CountDownLatch(1);
        final String[] holder = new String[1];

        rolesRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Object v = snapshot.getValue();
                    if (v != null) holder[0] = v.toString();
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                log.warn("rolesRef read cancelled: {}", error.getMessage());
                latch.countDown();
            }
        });

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for rolesRef read", e);
        }

        String role = holder[0];
        cache.put(uid, new CachedRole(role, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(CACHE_TTL_SECONDS)));
        return role;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, CachedRole> e : cache.entrySet()) {
            if (e.getValue().expiry < now) cache.remove(e.getKey());
        }
    }

    private static class CachedRole {
        final String role;
        final long expiry;
        CachedRole(String role, long expiry) { this.role = role; this.expiry = expiry; }
        boolean isExpired() { return System.currentTimeMillis() > expiry; }
    }
}
