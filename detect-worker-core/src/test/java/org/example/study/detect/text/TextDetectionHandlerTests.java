package org.example.study.detect.text.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectCommand;
import org.example.study.domain.DetectContext;
import org.example.study.domain.DetectExecutionMode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证文本归一化、规则证据累计与最终决策。 */
class TextDetectionHandlerTests {

    @Test
    void normalizesTextAndBuildsStableSha256() {
        TextNormalizeHandler handler = new TextNormalizeHandler();
        DetectContext first = context("  ＡBC\u200B   赌博  ", DetectExecutionMode.SYNC);
        DetectContext second = context("abc 赌博", DetectExecutionMode.SYNC);

        handler.handle(first, definition("normalize", handler));
        handler.handle(second, definition("normalize", handler));

        assertThat(first.getNormalizedText()).isEqualTo("abc 赌博");
        assertThat(first.getContentHash()).hasSize(64).isEqualTo(second.getContentHash());
        assertThat(first.getContentHash()).doesNotContain("赌博");
    }

    @Test
    void aggregatesRuleEvidenceBeforeFinalDecision() {
        DetectContext context = context("涉及赌博，请联系13800138000", DetectExecutionMode.SYNC);
        TextNormalizeHandler normalizeHandler = new TextNormalizeHandler();
        TextKeywordHandler keywordHandler = new TextKeywordHandler();
        TextRegexHandler regexHandler = new TextRegexHandler();
        TextDecisionHandler decisionHandler = new TextDecisionHandler();

        normalizeHandler.handle(context, definition("normalize", normalizeHandler));
        var keywordResult = keywordHandler.handle(context, definition("keyword", keywordHandler));
        var regexResult = regexHandler.handle(context, definition("regex", regexHandler));
        var decisionResult = decisionHandler.handle(context, definition("decision", decisionHandler));

        assertThat(keywordResult.resultCode()).isEqualTo("EVIDENCE");
        assertThat(regexResult.resultCode()).isEqualTo("EVIDENCE");
        assertThat(context.decision()).isEqualTo(DetectAction.REJECT);
        assertThat(decisionResult.resultCode()).isEqualTo("REJECT");
        assertThat(decisionResult.tags()).containsExactly("keyword_risk", "contact_risk");
    }

    private DetectContext context(String text, DetectExecutionMode mode) {
        return new DetectContext(new DetectCommand("task", "client", "trace", "app", "scene", ContentType.TEXT, null, null, mode, Instant.now(),
                java.util.List.of(new org.example.study.domain.DetectItem("one", text, null, null, "", ""))));
    }

    private ChainHandlerDefinition definition(String name, ChainHandler<?> handler) {
        return new ChainHandlerDefinition(name, null, handler, Map.of());
    }
}
