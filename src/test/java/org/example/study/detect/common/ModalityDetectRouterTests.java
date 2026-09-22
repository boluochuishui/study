package org.example.study.detect.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.example.study.baseSdk.chain.ChainExecuteResult;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectContext;
import org.example.study.domain.DetectRequest;
import org.junit.jupiter.api.Test;

class ModalityDetectRouterTests {

    @Test
    void shouldRouteToMatchingModalityExecutor() {
        ModalityDetectExecutor textExecutor = mock(ModalityDetectExecutor.class);
        DetectContext context = textContext();
        ChainExecuteResult expected = new ChainExecuteResult(
                context.traceId(), context.taskId(), "text.detect.chain", true, List.of(), 1L);
        when(textExecutor.supportType()).thenReturn(ContentType.TEXT);
        when(textExecutor.execute(context)).thenReturn(expected);
        ModalityDetectRouter router = new ModalityDetectRouter(List.of(textExecutor));

        assertThat(router.execute(ContentType.TEXT, context)).isSameAs(expected);
        verify(textExecutor).execute(context);
    }

    @Test
    void shouldRejectUnsupportedModality() {
        ModalityDetectRouter router = new ModalityDetectRouter(List.of());

        assertThatThrownBy(() -> router.execute(ContentType.TEXT, textContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported detection content type: TEXT");
    }

    @Test
    void shouldRejectDuplicatedExecutor() {
        ModalityDetectExecutor first = mock(ModalityDetectExecutor.class);
        ModalityDetectExecutor second = mock(ModalityDetectExecutor.class);
        when(first.supportType()).thenReturn(ContentType.TEXT);
        when(second.supportType()).thenReturn(ContentType.TEXT);

        assertThatThrownBy(() -> new ModalityDetectRouter(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Duplicated modality executor: TEXT");
    }

    private DetectContext textContext() {
        return new DetectContext(
                "trace-1", "task-1", "app-1",
                new DetectRequest("request-1", "comment", "TEXT", "normal", null, null, null));
    }
}
