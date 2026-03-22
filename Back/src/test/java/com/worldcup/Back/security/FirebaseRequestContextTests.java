package com.worldcup.Back.security;

import com.worldcup.Back.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FirebaseRequestContextTests {

    @Test
    void requireUidReturnsUidWhenPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(FirebaseRequestContext.UID_ATTR, "uid-123");

        String uid = FirebaseRequestContext.requireUid(request);

        assertEquals("uid-123", uid);
    }

    @Test
    void requireUidThrowsUnauthorizedWhenMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThrows(UnauthorizedException.class, () -> FirebaseRequestContext.requireUid(request));
    }

    @Test
    void getEmailReturnsNullWhenMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertNull(FirebaseRequestContext.getEmail(request));
    }
}
