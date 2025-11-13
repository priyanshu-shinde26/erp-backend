package com.erp.erpbackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

@Component
public class FirebaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseInitializer.class);

    // Replace with your RTDB URL (same you used earlier)
    private static final String DATABASE_URL = "https://campus-sync-firebase-default-rtdb.asia-southeast1.firebasedatabase.app";

    @PostConstruct
    public void init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                logger.info("Initializing FirebaseApp from service account resource...");
                InputStream serviceAccount = getClass().getClassLoader()
                        .getResourceAsStream("firebase-service-account.json");
                if (serviceAccount == null) {
                    logger.error("firebase-service-account.json not found in classpath (src/main/resources).");
                    return;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl(DATABASE_URL)
                        .build();

                FirebaseApp.initializeApp(options);
                logger.info("Firebase Admin initialized.");
            } else {
                logger.info("FirebaseApp already initialized, skipping.");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize FirebaseApp: {}", e.getMessage(), e);
        }
    }
}
