package org.example.study.baseSdk.lifecycle;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.example.study.baseSdk.lifecycle.web.LifecycleAdmissionFilter;
import org.example.study.baseSdk.lifecycle.web.LifecycleController;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证健康状态、HTTP 准入及摘流操作的权限边界。 */
class LifecycleWebTests {
    @Test
    void drainRequiresLoopbackAndValidTokenAndCannotReviveService() {
        var properties = new LifecycleProperties();
        properties.setDrainToken("test-drain-token");
        var lifecycle = new ServiceLifecycleManager(List.of(), properties);
        var controller = new LifecycleController(lifecycle, properties);
        assertThat(controller.ready().getStatusCode().value()).isEqualTo(503);
        lifecycle.ready();
        assertThat(controller.ready().getStatusCode().value()).isEqualTo(200);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");
        assertThat(controller.drain(request, "test-drain-token").getStatusCode().value()).isEqualTo(403);
        request.setRemoteAddr("127.0.0.1");
        assertThat(controller.drain(request, "wrong").getStatusCode().value()).isEqualTo(403);
        assertThat(controller.drain(request, "test-drain-token").getStatusCode().value()).isEqualTo(200);
        lifecycle.ready();
        assertThat(controller.ready().getStatusCode().value()).isEqualTo(503);
        assertThat(controller.live()).containsEntry("status", "UP");
    }

    @Test
    void blankTokenDisablesManualDrain() {
        var properties = new LifecycleProperties();
        var lifecycle = new ServiceLifecycleManager(List.of(), properties);
        var controller = new LifecycleController(lifecycle, properties);
        assertThat(controller.drain(new MockHttpServletRequest(), "").getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void filterRejectsNewRequestsAndReleasesAcceptedRequestOnFailure() throws Exception {
        var lifecycle = new ServiceLifecycleManager(List.of(), new LifecycleProperties());
        var filter = new LifecycleAdmissionFilter(lifecycle);
        var request = new MockHttpServletRequest("POST", "/api/detect/sync");
        var response = new MockHttpServletResponse();
        AtomicInteger called = new AtomicInteger();
        filter.doFilter(request, response, (incoming, outgoing) -> called.incrementAndGet());
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(called.get()).isZero();
        lifecycle.ready();
        filter.doFilter(request, new MockHttpServletResponse(), (incoming, outgoing) -> {
            assertThat(lifecycle.inFlight()).isEqualTo(1);
            lifecycle.beginDrain();
            called.incrementAndGet();
        });
        assertThat(called.get()).isEqualTo(1);
        assertThat(lifecycle.inFlight()).isZero();
    }
}
