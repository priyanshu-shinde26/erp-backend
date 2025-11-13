package com.erp.erpbackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    private final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

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
                    // Use your Realtime DB URL here:
                    .setDatabaseUrl("https://campus-sync-firebase-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp app = FirebaseApp.initializeApp(options);
                log.info("Firebase Admin initialized.");
                return app;
            } else {
                FirebaseApp app = FirebaseApp.getInstance();
                log.info("Firebase Admin already initialized.");
                return app;
            }
        } catch (Exception e) {
            log.error("Failed to initialize FirebaseApp", e);
            throw new RuntimeException(e);
        }
    }

    @Bean
    public FirebaseDatabase firebaseDatabase(FirebaseApp app) {
        FirebaseDatabase db = FirebaseDatabase.getInstance(app);
        log.info("FirebaseDatabase bean created");
        return db;
    }

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
}
