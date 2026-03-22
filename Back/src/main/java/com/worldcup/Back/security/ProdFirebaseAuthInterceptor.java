package com.worldcup.Back.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class ProdFirebaseAuthInterceptor implements RequestAuthInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ProdFirebaseAuthInterceptor.class);
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${auth.firebase.check-revoked:false}")
    private boolean checkRevoked;

    @Value("${auth.firebase.project-id:}")
    private String firebaseProjectId;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "Missing or invalid Authorization header", null, null);
            return false;
        }

        String idToken = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (idToken.isBlank()) {
            writeUnauthorized(response, "Missing Firebase ID token", null, null);
            return false;
        }

        try {
            FirebaseToken decodedToken = checkRevoked
                    ? FirebaseAuth.getInstance().verifyIdToken(idToken, true)
                    : FirebaseAuth.getInstance().verifyIdToken(idToken);
            request.setAttribute(FirebaseRequestContext.UID_ATTR, decodedToken.getUid());
            request.setAttribute(FirebaseRequestContext.EMAIL_ATTR, decodedToken.getEmail());
            return true;
        } catch (FirebaseAuthException ex) {
            String authErrorCode = ex.getAuthErrorCode() != null ? ex.getAuthErrorCode().name() : "UNKNOWN";
            String detail = buildTokenDebugDetail(idToken, ex.getMessage());
            log.warn("Firebase token rejected. reason='{}' message='{}' uri='{}' method='{}'",
                    ex.getAuthErrorCode(), ex.getMessage(), request.getRequestURI(), request.getMethod());
            writeUnauthorized(response, "Invalid or expired Firebase token", authErrorCode, detail);
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message, String authErrorCode, String detail) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (authErrorCode != null && !authErrorCode.isBlank()) {
            response.setHeader("X-Auth-Error-Code", authErrorCode);
            if (detail != null && !detail.isBlank()) {
                response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\",\"authErrorCode\":\"" + authErrorCode + "\",\"detail\":\"" + escapeJson(detail) + "\"}");
                return;
            }

            response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\",\"authErrorCode\":\"" + authErrorCode + "\"}");
            return;
        }

        response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }

    private String buildTokenDebugDetail(String idToken, String originalMessage) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                return originalMessage;
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String aud = extractJsonString(payloadJson, "aud");
            String iss = extractJsonString(payloadJson, "iss");

            if (firebaseProjectId != null && !firebaseProjectId.isBlank() && aud != null && !firebaseProjectId.equals(aud)) {
                return "Token project mismatch: token aud='" + aud + "', backend FIREBASE_PROJECT_ID='" + firebaseProjectId + "'";
            }

            if (iss != null && firebaseProjectId != null && !firebaseProjectId.isBlank() && !iss.endsWith("/" + firebaseProjectId)) {
                return "Token issuer mismatch: token iss='" + iss + "', expected suffix='/' + FIREBASE_PROJECT_ID";
            }

            return originalMessage;
        } catch (Exception ignored) {
            return originalMessage;
        }
    }

    private String extractJsonString(String json, String key) {
        String token = "\"" + key + "\":\"";
        int start = json.indexOf(token);
        if (start < 0) {
            return null;
        }
        int valueStart = start + token.length();
        int valueEnd = json.indexOf('"', valueStart);
        if (valueEnd < 0) {
            return null;
        }
        return json.substring(valueStart, valueEnd);
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
