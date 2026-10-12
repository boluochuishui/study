package org.example.study.baseSdk.lifecycle.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.study.baseSdk.lifecycle.ServiceLifecycleManager;
import org.example.study.baseSdk.lifecycle.ServiceUnavailableException;
import org.springframework.web.filter.OncePerRequestFilter;

/** 在鉴权之前拦截业务流量，并持有请求租约直到响应处理结束。 */
public class LifecycleAdmissionFilter extends OncePerRequestFilter {
    private final ServiceLifecycleManager lifecycle;

    public LifecycleAdmissionFilter(ServiceLifecycleManager lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ServiceLifecycleManager.WorkLease lease;
        try {
            lease = lifecycle.acquire();
        } catch (ServiceUnavailableException exception) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json");
            response.setHeader("Retry-After", "1");
            response.getWriter().write("{\"errorCode\":\"SERVICE_UNAVAILABLE\",\"message\":\"Service is not accepting new requests\"}");
            return;
        }
        try (lease) {
            chain.doFilter(request, response);
        }
    }
}
