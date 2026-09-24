package org.example.study.baseSdk.rule.spel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * 验证通用 SpEL 规则的编译、安全边界和执行策略。
 */
class SpelRuleEngineTests {

    private final SpelRuleEngine engine = new SpelRuleEngine();

    @Test
    void autoConfigurationProvidesDefaultBeanAndAllowsOverride() {
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SpelRuleAutoConfiguration.class));

        contextRunner.run(context -> assertThat(context).hasSingleBean(SpelRuleEngine.class));

        SpelRuleEngine customEngine = new SpelRuleEngine(10, 100);
        contextRunner.withBean(SpelRuleEngine.class, () -> customEngine)
                .run(context -> assertThat(context.getBean(SpelRuleEngine.class)).isSameAs(customEngine));
    }

    @Test
    void evaluatesEnabledRulesByPriorityAndCollectsAllMatches() {
        SpelRuleSet ruleSet = engine.compile(List.of(
                rule("later", "#score >= 80", 20, true, Map.of("action", "REJECT")),
                rule("disabled", "true", 1, false, Map.of()),
                rule("first", "#contentLength > 10", 10, true, Map.of("action", "REVIEW"))));

        SpelRuleEvaluationResult result = ruleSet.evaluate(null,
                Map.of("score", 90, "contentLength", 20), SpelRuleEvaluationMode.ALL_MATCHES,
                SpelRuleErrorPolicy.FAIL_FAST);

        assertThat(ruleSet.size()).isEqualTo(2);
        assertThat(result.matched()).isTrue();
        assertThat(result.matches()).extracting(SpelRuleMatch::ruleId).containsExactly("first", "later");
        assertThat(result.matches().getFirst().attributes()).containsEntry("action", "REVIEW");
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void firstMatchStopsRemainingRules() {
        SpelRuleSet ruleSet = engine.compile(List.of(
                rule("first", "true", 1, true, Map.of()),
                rule("would-fail", "#missing > 1", 2, true, Map.of())));

        SpelRuleEvaluationResult result = ruleSet.evaluate(null, Map.of(), SpelRuleEvaluationMode.FIRST_MATCH,
                SpelRuleErrorPolicy.FAIL_FAST);

        assertThat(result.matches()).extracting(SpelRuleMatch::ruleId).containsExactly("first");
    }

    @Test
    void supportsRootObjectProperties() {
        SpelRuleSet ruleSet = engine.compile(List.of(
                rule("root", "level == 'HIGH' and enabled", 1, true, Map.of())));

        SpelRuleEvaluationResult result = ruleSet.evaluate(new RuleFacts("HIGH", true), Map.of(),
                SpelRuleEvaluationMode.ALL_MATCHES, SpelRuleErrorPolicy.FAIL_FAST);

        assertThat(result.matched()).isTrue();
    }

    @Test
    void skipFailedRecordsErrorAndContinues() {
        SpelRuleSet ruleSet = engine.compile(List.of(
                rule("broken", "'not-boolean'", 1, true, Map.of()),
                rule("matched", "true", 2, true, Map.of())));

        SpelRuleEvaluationResult result = ruleSet.evaluate(null, Map.of(), SpelRuleEvaluationMode.ALL_MATCHES,
                SpelRuleErrorPolicy.SKIP_FAILED);

        assertThat(result.errors()).extracting(SpelRuleError::ruleId).containsExactly("broken");
        assertThat(result.matches()).extracting(SpelRuleMatch::ruleId).containsExactly("matched");
    }

    @Test
    void failFastWrapsEvaluationError() {
        SpelRuleSet ruleSet = engine.compile(List.of(
                rule("not-boolean", "'text'", 1, true, Map.of())));

        assertThatThrownBy(() -> ruleSet.evaluate(null, Map.of(), SpelRuleEvaluationMode.ALL_MATCHES,
                SpelRuleErrorPolicy.FAIL_FAST))
                .isInstanceOf(SpelRuleEvaluationException.class)
                .hasMessage("Failed to evaluate SpEL rule: not-boolean");
    }

    @Test
    void rejectsInvalidDefinitionsAndSyntax() {
        assertThatThrownBy(() -> engine.compile(List.of(
                rule("same", "true", 1, true, Map.of()),
                rule("same", "false", 2, true, Map.of()))))
                .isInstanceOf(SpelRuleCompilationException.class)
                .hasMessage("Duplicated SpEL rule ID: same");

        assertThatThrownBy(() -> engine.compile(List.of(rule("syntax", "#score >", 1, true, Map.of()))))
                .isInstanceOf(SpelRuleCompilationException.class)
                .hasMessage("Invalid SpEL rule expression: syntax");
    }

    @Test
    void rejectsDangerousExpressionFeatures() {
        List<String> expressions = List.of(
                "T(java.lang.Runtime).getRuntime() != null",
                "new java.lang.String('x') == 'x'",
                "@dataSource != null",
                "#content.toUpperCase() == 'X'");

        for (String expression : expressions) {
            assertThatThrownBy(() -> engine.compile(List.of(rule("unsafe", expression, 1, true, Map.of()))))
                    .isInstanceOf(SpelRuleCompilationException.class)
                    .hasMessageStartingWith("Forbidden SpEL feature in rule unsafe:");
        }
    }

    @Test
    void enforcesConfiguredLimitsAndCopiesAttributes() {
        SpelRuleEngine limitedEngine = new SpelRuleEngine(1, 5);
        Map<String, String> mutableAttributes = new java.util.HashMap<>();
        mutableAttributes.put("action", "PASS");
        SpelRuleDefinition definition = rule("limit", "true", 1, true, mutableAttributes);
        mutableAttributes.put("action", "REJECT");

        SpelRuleEvaluationResult result = limitedEngine.compile(List.of(definition)).evaluate(null, Map.of(),
                SpelRuleEvaluationMode.ALL_MATCHES, SpelRuleErrorPolicy.FAIL_FAST);

        assertThat(result.matches().getFirst().attributes()).containsEntry("action", "PASS");
        assertThatThrownBy(() -> limitedEngine.compile(List.of(rule("long", "#a == 1", 1, true, Map.of()))))
                .isInstanceOf(SpelRuleCompilationException.class)
                .hasMessage("SpEL rule expression exceeds configured limit: long");
    }

    private SpelRuleDefinition rule(String ruleId, String expression, int priority, boolean enabled,
                                    Map<String, String> attributes) {
        return new SpelRuleDefinition(ruleId, expression, priority, enabled, attributes);
    }

    private record RuleFacts(String level, boolean enabled) {
    }
}
