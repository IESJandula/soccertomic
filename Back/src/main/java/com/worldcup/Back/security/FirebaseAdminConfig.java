package com.worldcup.Back.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseAdminConfig {

    @Value("${auth.firebase.enabled:true}")
    private boolean firebaseEnabled;

    @Value("${auth.firebase.project-id}")
    private String firebaseProjectId;

    @Value("${auth.firebase.credentials-path:../variables-entorno/firebase-service-account.json}")
    private String firebaseCredentialsPath;

    @Value("${auth.firebase.credentials-json:}")
    private String firebaseCredentialsJson;

    @PostConstruct
    public void initFirebaseAdmin() throws IOException {
        if (!firebaseEnabled || !FirebaseApp.getApps().isEmpty()) {
            return;
        }

        GoogleCredentials credentials = loadCredentials();

        FirebaseOptions.Builder builder = FirebaseOptions.builder()
                .setCredentials(credentials);

        if (firebaseProjectId != null && !firebaseProjectId.isBlank()) {
            builder.setProjectId(firebaseProjectId);
        }

        FirebaseApp.initializeApp(builder.build());
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (firebaseCredentialsJson != null && !firebaseCredentialsJson.isBlank()) {
            return GoogleCredentials.fromStream(
                new ByteArrayInputStream(firebaseCredentialsJson.getBytes(StandardCharsets.UTF_8))
            );
        }

        if (firebaseCredentialsPath == null || firebaseCredentialsPath.isBlank()) {
            throw new IllegalStateException(
                "Firebase credentials required. Set auth.firebase.credentials-json or auth.firebase.credentials-path"
            );
        }

        return GoogleCredentials.fromStream(new FileInputStream(firebaseCredentialsPath));
    }
}
