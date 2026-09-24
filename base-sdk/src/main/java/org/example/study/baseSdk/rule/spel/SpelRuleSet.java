package org.example.study.baseSdk.rule.spel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

/**
 * 已预编译的不可变规则快照，可被多个请求线程并发执行。
 */
public final class SpelRuleSet {

    private final List<CompiledRule> rules;

    SpelRuleSet(List<CompiledRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public int size() {
        return rules.size();
    }

    /**
     * 每次执行创建独立上下文，避免变量在并发请求间相互污染。
     */
    public SpelRuleEvaluationResult evaluate(Object rootObject, Map<String, ?> variables,
                                             SpelRuleEvaluationMode mode, SpelRuleErrorPolicy errorPolicy) {
        Objects.requireNonNull(variables, "SpEL variables must not be null");
        Objects.requireNonNull(mode, "SpEL rule evaluation mode must not be null");
        Objects.requireNonNull(errorPolicy, "SpEL rule error policy must not be null");
        SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
        variables.forEach(context::setVariable);
        List<SpelRuleMatch> matches = new ArrayList<>();
        List<SpelRuleError> errors = new ArrayList<>();
        for (CompiledRule rule : rules) {
            try {
                Boolean matched = rule.expression().getValue(context, rootObject, Boolean.class);
                if (matched == null) {
                    throw new EvaluationException("SpEL rule result must not be null");
                }
                if (matched) {
                    SpelRuleDefinition definition = rule.definition();
                    matches.add(new SpelRuleMatch(definition.ruleId(), definition.priority(),
                            definition.attributes()));
                    if (mode == SpelRuleEvaluationMode.FIRST_MATCH) {
                        break;
                    }
                }
            } catch (EvaluationException exception) {
                handleEvaluationError(rule.definition().ruleId(), exception, errorPolicy, errors);
            }
        }
        return new SpelRuleEvaluationResult(matches, errors);
    }

    private void handleEvaluationError(String ruleId, EvaluationException exception,
                                       SpelRuleErrorPolicy errorPolicy, List<SpelRuleError> errors) {
        String message = "Failed to evaluate SpEL rule: " + ruleId;
        if (errorPolicy == SpelRuleErrorPolicy.FAIL_FAST) {
            throw new SpelRuleEvaluationException(ruleId, message, exception);
        }
        errors.add(new SpelRuleError(ruleId, exception.getClass().getSimpleName(), message));
    }

    record CompiledRule(SpelRuleDefinition definition, Expression expression) {
    }
}
