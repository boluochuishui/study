package org.example.study.baseSdk.auth;

import org.example.study.baseSdk.log.LogRecorder;
import org.example.study.baseSdk.log.CompositeLogRecorder;
import org.example.study.baseSdk.log.Slf4jLogRecorder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** Web 服务显式启用鉴权时装配通用过滤器与默认 Provider。 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "study.auth", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(AuthWebProperties.class)
public class AuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LogRecorder.class)
    public LogRecorder authFallbackLogRecorder() {
        return new CompositeLogRecorder(List.of(new Slf4jLogRecorder()));
    }

    @Bean
    @ConditionalOnMissingBean(AuthRuleMatcher.class)
    public SnapshotAuthRuleMatcher authRuleMatcher(AuthConfigSource source, AuthConfigSnapshot bootstrap) {
        return new SnapshotAuthRuleMatcher(source, bootstrap);
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationManager.class)
    public AuthenticationManager authenticationManager(List<AuthenticationProvider> providers) {
        return new AuthenticationManager(providers);
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationLogRecorder.class)
    public AuthenticationLogRecorder authenticationLogRecorder(LogRecorder recorder) {
        return new AuthenticationLogRecorder(recorder);
    }

    @Bean
    @ConditionalOnMissingBean(HmacSignatureService.class)
    public HmacSignatureService hmacSignatureService() {
        return new HmacSignatureService();
    }

    @Bean
    @ConditionalOnMissingBean(NonceStore.class)
    public InMemoryNonceStore nonceStore() {
        return new InMemoryNonceStore();
    }

    @Bean
    @ConditionalOnMissingBean(CredentialProvider.class)
    public EnvironmentCredentialProvider credentialProvider(
            @Value("${study.auth.app-id}") String appId,
            @Value("${study.auth.app-secret}") String appSecret) {
        return new EnvironmentCredentialProvider(appId, appSecret);
    }

    @Bean
    @ConditionalOnMissingBean(AnonymousAuthenticationProvider.class)
    public AnonymousAuthenticationProvider anonymousAuthenticationProvider() {
        return new AnonymousAuthenticationProvider();
    }

    @Bean
    @ConditionalOnMissingBean(SignatureAuthenticationProvider.class)
    public SignatureAuthenticationProvider signatureAuthenticationProvider(
            CredentialProvider credentialProvider, NonceStore nonceStore, HmacSignatureService signatureService,
            @Value("${study.auth.clock-skew-seconds:300}") long clockSkewSeconds,
            @Value("${study.auth.nonce-ttl-seconds:600}") long nonceTtlSeconds) {
        return new SignatureAuthenticationProvider(credentialProvider, nonceStore, signatureService,
                clockSkewSeconds, nonceTtlSeconds);
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationFilter.class)
    public AuthenticationFilter authenticationFilter(AuthRuleMatcher matcher, AuthenticationManager manager,
                                                     AuthenticationLogRecorder recorder, ObjectMapper objectMapper,
                                                     AuthWebProperties properties) {
        return new AuthenticationFilter(matcher, manager, recorder, objectMapper, properties);
    }
}
