package com.erp.erpbackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.StorageClient;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseConfig implements ApplicationListener<ContextClosedEvent> {

    private final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);
    private FirebaseApp firebaseApp;

    /**
     * ✅ UPDATED: Initialize Firebase using @PostConstruct
     * This ensures Firebase is ready before any beans are requested.
     */
    @PostConstruct
    public void init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                // ✅ Use ClassPathResource to find the file in src/main/resources
                org.springframework.core.io.ClassPathResource resource =
                        new org.springframework.core.io.ClassPathResource("firebase-service-account.json");

                InputStream serviceAccount = resource.getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl("https://campus-sync-firebase-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .build();

                firebaseApp = FirebaseApp.initializeApp(options);
                log.info("✅ Firebase Admin initialized via ClassPath.");
            }
        } catch (Exception e) {
            log.error("❌ Firebase initialization failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Expose FirebaseApp as a bean
     */
    @Bean
    public FirebaseApp firebaseApp() {
        return firebaseApp;
    }

    /**
     * Expose FirebaseAuth so your controllers/services can verify tokens
     */
    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp app) {
        FirebaseAuth auth = FirebaseAuth.getInstance(app);
        log.info("FirebaseAuth bean created");
        return auth;
    }

    /**
     * Expose Firebase Realtime Database
     */
    @Bean
    public FirebaseDatabase firebaseDatabase(FirebaseApp app) {
        FirebaseDatabase db = FirebaseDatabase.getInstance(app);
        log.info("FirebaseDatabase bean created");
        return db;
    }

    /**
     * Expose StorageClient
     */
    @Bean
    public StorageClient storageClient(FirebaseApp app) {
        try {
            StorageClient client = StorageClient.getInstance(app);
            log.info("StorageClient bean created");
            return client;
        } catch (Exception e) {
            log.warn("StorageClient creation failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * DatabaseReference beans
     */
    @Bean(name = "studentsRef")
    public DatabaseReference studentsRef(FirebaseDatabase database) {
        DatabaseReference ref = database.getReference("/students");
        log.info("studentsRef DatabaseReference bean created");
        return ref;
    }

    @Bean(name = "rolesRef")
    public DatabaseReference rolesRef(FirebaseDatabase database) {
        DatabaseReference ref = database.getReference("/roles");
        log.info("rolesRef DatabaseReference bean created");
        return ref;
    }

    @Bean(name = "attendanceRef")
    public DatabaseReference attendanceRef(FirebaseDatabase firebaseDatabase) {
        DatabaseReference ref = firebaseDatabase.getReference("attendance");
        log.info("attendanceRef DatabaseReference bean created");
        return ref;
    }

    /**
     * Graceful shutdown
     */
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        if (firebaseApp != null) {
            try {
                log.info("Shutting down FirebaseApp...");
                firebaseApp.delete();
            } catch (Exception e) {
                log.warn("Error deleting FirebaseApp during shutdown", e);
            }
        }
    }
}