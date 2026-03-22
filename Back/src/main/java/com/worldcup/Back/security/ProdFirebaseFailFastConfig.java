package com.worldcup.Back.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class ProdFirebaseFailFastConfig {

    @Value("${auth.firebase.enabled:false}")
    private boolean firebaseEnabled;

    @Value("${auth.firebase.fail-fast:true}")
    private boolean failFast;

    @Value("${auth.firebase.project-id:}")
    private String firebaseProjectId;

    @Value("${auth.firebase.credentials-path:}")
    private String firebaseCredentialsPath;

    @PostConstruct
    public void validate() {
        if (!failFast) {
            return;
        }
        if (!firebaseEnabled) {
            throw new IllegalStateException("auth.firebase.enabled must be true");
        }
        if (firebaseProjectId == null || firebaseProjectId.isBlank()) {
            throw new IllegalStateException("Missing FIREBASE_PROJECT_ID");
        }
        if (firebaseProjectId.contains("[") || firebaseProjectId.contains("]")) {
            throw new IllegalStateException("Invalid FIREBASE_PROJECT_ID placeholder. Set a real Firebase project id");
        }

        boolean hasPath = firebaseCredentialsPath != null && !firebaseCredentialsPath.isBlank();
        if (!hasPath) {
            throw new IllegalStateException("Missing FIREBASE_CREDENTIALS_PATH");
        }

        File credentialsFile = new File(firebaseCredentialsPath);
        if (!credentialsFile.exists() || !credentialsFile.isFile()) {
            throw new IllegalStateException("Firebase credentials file not found: " + firebaseCredentialsPath);
        }
    }
}
