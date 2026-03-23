package org.example.sdpclient.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class CookieUtils {

    private CookieUtils() {}

    public static String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) return cookie.getValue();
        }
        return null;
    }

    public static boolean isRootAdmin(HttpServletRequest request) {
        String rootStr = getCookieValue(request, "adminRoot");
        return "true".equalsIgnoreCase(rootStr);
    }

    public static Long getAdminIdFromCookie(HttpServletRequest request) {
        String idStr = getCookieValue(request, "adminId");
        if (idStr == null || idStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin ID");
        }
    }

    public static Long getAdminIdOrNull(HttpServletRequest request) {
        String idStr = getCookieValue(request, "adminId");
        if (idStr == null || idStr.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
