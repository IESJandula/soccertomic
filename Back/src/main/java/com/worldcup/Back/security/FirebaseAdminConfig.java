package com.worldcup.Back.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseAdminConfig {

    @Value("${auth.firebase.enabled:true}")
    private boolean firebaseEnabled;

    @Value("${auth.firebase.project-id}")
    private String firebaseProjectId;

    @Value("${auth.firebase.credentials-path:../variables-entorno/firebase-service-account.json}")
    private String firebaseCredentialsPath;

    @PostConstruct
    public void initFirebaseAdmin() throws IOException {
        if (!firebaseEnabled || !FirebaseApp.getApps().isEmpty()) {
            return;
        }

        if (firebaseCredentialsPath == null || firebaseCredentialsPath.isBlank()) {
            throw new IllegalStateException(
                "Firebase credentials required. Set FIREBASE_CREDENTIALS_PATH environment variable or place firebase-service-account.json in variables-entorno/"
            );
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(firebaseCredentialsPath));

        FirebaseOptions.Builder builder = FirebaseOptions.builder()
                .setCredentials(credentials);

        if (firebaseProjectId != null && !firebaseProjectId.isBlank()) {
            builder.setProjectId(firebaseProjectId);
        }

        FirebaseApp.initializeApp(builder.build());
    }
}
