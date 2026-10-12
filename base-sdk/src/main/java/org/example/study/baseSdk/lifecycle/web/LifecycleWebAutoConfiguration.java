package org.example.study.baseSdk.lifecycle.web;

import org.example.study.baseSdk.lifecycle.LifecycleAutoConfiguration;
import org.example.study.baseSdk.lifecycle.LifecycleProperties;
import org.example.study.baseSdk.lifecycle.ServiceLifecycleManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/** Servlet 服务自动接入健康探针和请求准入过滤器。 */
@AutoConfiguration(after = LifecycleAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class LifecycleWebAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(LifecycleController.class)
    public LifecycleController lifecycleController(ServiceLifecycleManager lifecycle, LifecycleProperties properties) {
        return new LifecycleController(lifecycle, properties);
    }

    @Bean
    public FilterRegistrationBean<LifecycleAdmissionFilter> lifecycleAdmissionFilter(ServiceLifecycleManager lifecycle) {
        FilterRegistrationBean<LifecycleAdmissionFilter> registration = new FilterRegistrationBean<>(new LifecycleAdmissionFilter(lifecycle));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
