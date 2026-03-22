package com.worldcup.Back.security;

import com.worldcup.Back.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;

public final class FirebaseRequestContext {

    public static final String UID_ATTR = "firebaseUid";
    public static final String EMAIL_ATTR = "firebaseEmail";

    private FirebaseRequestContext() {
    }

    /**
     * Get user ID from request context.
     * Requires that an authentication interceptor already populated request attributes.
     *
     * @param request HTTP request
     * @return Authenticated Firebase UID
     */
    public static String requireUid(HttpServletRequest request) {
        Object uid = request.getAttribute(UID_ATTR);
        if (uid != null) {
            return uid.toString();
        }

        throw new UnauthorizedException("Authentication required");
    }

    /**
     * Get email from request context (optional).
     * Requires that an authentication interceptor already populated request attributes.
     *
     * @param request HTTP request
     * @return Email or null
     */
    public static String getEmail(HttpServletRequest request) {
        Object email = request.getAttribute(EMAIL_ATTR);
        if (email != null) {
            return email.toString();
        }

        return null;
    }
}
