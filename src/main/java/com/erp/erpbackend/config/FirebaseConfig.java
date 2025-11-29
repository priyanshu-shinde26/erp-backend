package com.erp.erpbackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.StorageClient;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

import java.io.InputStream;

@Configuration
public class FirebaseConfig implements ApplicationListener<ContextClosedEvent> {

    private final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);
    private FirebaseApp firebaseApp;

    /**
     * Initialize FirebaseApp from the service account JSON on the classpath:
     * src/main/resources/firebase-service-account.json
     */
    @Bean
    public FirebaseApp firebaseApp() {
        try (InputStream serviceAccount = getClass().getClassLoader()
                .getResourceAsStream("firebase-service-account.json")) {

            if (serviceAccount == null) {
                String msg = "FIREBASE CONFIG ERROR: service account file 'firebase-service-account.json' not found on classpath (src/main/resources).";
                log.error(msg);
                throw new IllegalStateException(msg);
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    // change to your RTDB URL if different:
                    .setDatabaseUrl("https://campus-sync-firebase-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                firebaseApp = FirebaseApp.initializeApp(options);
                log.info("Firebase Admin initialized (new app).");
            } else {
                firebaseApp = FirebaseApp.getInstance();
                log.info("Firebase Admin already initialized (existing app).");
            }
            return firebaseApp;
        } catch (Exception e) {
            log.error("Failed to initialize FirebaseApp", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Expose FirebaseAuth so your controllers/services can verify tokens:
     * @Autowired FirebaseAuth firebaseAuth
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
     * Optional: expose StorageClient if you plan to use Firebase Storage later.
     * If you don't set a storage bucket in your service account / FirebaseOptions,
     * this will still return an instance but attempts to access bucket may fail.
     */
    @Bean
    public StorageClient storageClient(FirebaseApp app) {
        try {
            StorageClient client = StorageClient.getInstance(app);
            log.info("StorageClient bean created");
            return client;
        } catch (Exception e) {
            log.warn("StorageClient creation failed or not configured: " + e.getMessage());
            return null;
        }
    }

    /**
     * Convenience named DatabaseReference beans (your code used these).
     * Feel free to adjust paths (/students, /roles, /attendance) as you keep in your RTDB.
     */
    @Bean(name = "studentsRef")
    public DatabaseReference studentsRef(FirebaseDatabase database) {
        DatabaseReference ref = database.getReference("/students");
        log.info("studentsRef DatabaseReference bean created (path = /students)");
        return ref;
    }

    @Bean(name = "rolesRef")
    public DatabaseReference rolesRef(FirebaseDatabase database) {
        DatabaseReference ref = database.getReference("/roles");
        log.info("rolesRef DatabaseReference bean created (path = /roles)");
        return ref;
    }

    @Bean(name = "attendanceRef")
    public DatabaseReference attendanceRef(FirebaseDatabase firebaseDatabase) {
        DatabaseReference ref = firebaseDatabase.getReference("attendance");
        log.info("attendanceRef DatabaseReference bean created (path = /attendance)");
        return ref;
    }

    /**
     * Graceful shutdown: delete FirebaseApp so underlying SDK threads stop cleanly.
     * This prevents the "web application appears to have started a thread ... failed to stop it" warnings.
     */
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        if (firebaseApp != null) {
            try {
                log.info("Shutting down FirebaseApp to stop SDK worker threads...");
                firebaseApp.delete();
            } catch (Exception e) {
                log.warn("Error deleting FirebaseApp during shutdown", e);
            }
        }
    }
}
