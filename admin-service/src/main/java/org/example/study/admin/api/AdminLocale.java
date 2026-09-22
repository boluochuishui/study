package org.example.study.admin.api;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;

/**
 * 第一版仅提供中文和英文，未指定语言时使用中文。
 */
public final class AdminLocale {

    private AdminLocale() {
    }

    public static Locale resolve(HttpServletRequest request) {
        if (request.getHeader("Accept-Language") == null) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        return "en".equalsIgnoreCase(request.getLocale().getLanguage())
                ? Locale.ENGLISH : Locale.SIMPLIFIED_CHINESE;
    }
}
