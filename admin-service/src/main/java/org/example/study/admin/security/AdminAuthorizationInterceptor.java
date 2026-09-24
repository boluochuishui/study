package org.example.study.admin.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.study.admin.api.AdminApiException;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** 执行控制器上的 RBAC 权限声明。 */
@Component
public class AdminAuthorizationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequireAdminPermission required = AnnotatedElementUtils.findMergedAnnotation(
                method.getMethod(), RequireAdminPermission.class);
        if (required == null) {
            required = AnnotatedElementUtils.findMergedAnnotation(
                    method.getBeanType(), RequireAdminPermission.class);
        }
        if (required != null && !AdminSecurityContext.requirePrincipal().hasPermission(required.value())) {
            throw new AdminApiException("FORBIDDEN", "Permission denied", HttpStatus.FORBIDDEN);
        }
        return true;
    }
}
