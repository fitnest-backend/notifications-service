package az.fitnest.notifications.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${app.firebase.config-path:/app/firebase/serviceAccountKey.json}")
    private String configPath;

    @Value("${app.firebase.enabled:true}")
    private boolean firebaseEnabled;

    private boolean initialized = false;

    @PostConstruct
    public void initialize() {
        if (!firebaseEnabled) {
            logger.info("Firebase is disabled");
            initialized = false;
            return;
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FileInputStream serviceAccount = new FileInputStream(configPath);

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                initialized = true;
                logger.info("Firebase initialized successfully");
            } else {
                initialized = true;
                logger.info("Firebase already initialized");
            }
        } catch (IOException e) {
            logger.warn("Failed to initialize Firebase: {}", e.getMessage(), e);
            initialized = false;
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (!initialized) {
            logger.warn("Firebase is not initialized. Make sure the service account key file exists at: {}", configPath);
            logger.warn("Push notifications will not be available.");
            return null;
        }
        return FirebaseMessaging.getInstance(FirebaseApp.getInstance());
    }
}
