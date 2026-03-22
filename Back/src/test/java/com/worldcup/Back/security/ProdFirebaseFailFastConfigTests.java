package com.worldcup.Back.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProdFirebaseFailFastConfigTests {

    @TempDir
    Path tempDir;

    @Test
    void validateThrowsWhenCredentialsPathMissing() {
        ProdFirebaseFailFastConfig config = baseConfig();
        ReflectionTestUtils.setField(config, "firebaseCredentialsPath", "");

        assertThrows(IllegalStateException.class, config::validate);
    }

    @Test
    void validateThrowsWhenCredentialsFileDoesNotExist() {
        ProdFirebaseFailFastConfig config = baseConfig();
        ReflectionTestUtils.setField(config, "firebaseCredentialsPath", tempDir.resolve("missing.json").toString());

        assertThrows(IllegalStateException.class, config::validate);
    }

    @Test
    void validatePassesWithExistingCredentialsFile() throws IOException {
        Path credentials = tempDir.resolve("firebase-service-account.json");
        Files.writeString(credentials, "{}");

        ProdFirebaseFailFastConfig config = baseConfig();
        ReflectionTestUtils.setField(config, "firebaseCredentialsPath", credentials.toString());

        assertDoesNotThrow(config::validate);
    }

    private ProdFirebaseFailFastConfig baseConfig() {
        ProdFirebaseFailFastConfig config = new ProdFirebaseFailFastConfig();
        ReflectionTestUtils.setField(config, "firebaseEnabled", true);
        ReflectionTestUtils.setField(config, "failFast", true);
        ReflectionTestUtils.setField(config, "firebaseProjectId", "appfutbol-a595c");
        return config;
    }
}
