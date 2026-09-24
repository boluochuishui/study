package org.example.study.baseSdk.rule.spel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * 校验并预编译规则定义，生成可并发复用的不可变规则集。
 */
public final class SpelRuleEngine {

    public static final int DEFAULT_MAX_RULE_COUNT = 500;
    public static final int DEFAULT_MAX_EXPRESSION_LENGTH = 2048;

    private static final Set<String> FORBIDDEN_NODE_TYPES = Set.of(
            "Assign", "BeanReference", "ConstructorReference", "FunctionReference", "MethodReference",
            "OpDec", "OpInc", "TypeReference");

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final int maxRuleCount;
    private final int maxExpressionLength;

    public SpelRuleEngine() {
        this(DEFAULT_MAX_RULE_COUNT, DEFAULT_MAX_EXPRESSION_LENGTH);
    }

    public SpelRuleEngine(int maxRuleCount, int maxExpressionLength) {
        if (maxRuleCount <= 0 || maxExpressionLength <= 0) {
            throw new IllegalArgumentException("SpEL rule limits must be greater than zero");
        }
        this.maxRuleCount = maxRuleCount;
        this.maxExpressionLength = maxExpressionLength;
    }

    /**
     * 编译全部启用规则；任意规则不合法时整批失败，便于热加载保留旧快照。
     */
    public SpelRuleSet compile(Collection<SpelRuleDefinition> definitions) {
        Objects.requireNonNull(definitions, "SpEL rule definitions must not be null");
        if (definitions.size() > maxRuleCount) {
            throw new SpelRuleCompilationException("SpEL rule count exceeds configured limit");
        }
        Set<String> ruleIds = new HashSet<>();
        List<SpelRuleSet.CompiledRule> compiledRules = new ArrayList<>();
        for (SpelRuleDefinition definition : definitions) {
            validateDefinition(definition, ruleIds);
            if (definition.enabled()) {
                compiledRules.add(compileRule(definition));
            }
        }
        compiledRules.sort(Comparator.comparingInt((SpelRuleSet.CompiledRule rule) -> rule.definition().priority())
                .thenComparing(rule -> rule.definition().ruleId()));
        return new SpelRuleSet(compiledRules);
    }

    private void validateDefinition(SpelRuleDefinition definition, Set<String> ruleIds) {
        if (definition == null) {
            throw new SpelRuleCompilationException("SpEL rule definition must not be null");
        }
        if (definition.ruleId() == null || definition.ruleId().isBlank()) {
            throw new SpelRuleCompilationException("SpEL rule ID must not be blank");
        }
        if (!ruleIds.add(definition.ruleId())) {
            throw new SpelRuleCompilationException("Duplicated SpEL rule ID: " + definition.ruleId());
        }
        if (definition.expression() == null || definition.expression().isBlank()) {
            throw new SpelRuleCompilationException("SpEL rule expression must not be blank: " + definition.ruleId());
        }
        if (definition.expression().length() > maxExpressionLength) {
            throw new SpelRuleCompilationException("SpEL rule expression exceeds configured limit: "
                    + definition.ruleId());
        }
    }

    private SpelRuleSet.CompiledRule compileRule(SpelRuleDefinition definition) {
        try {
            Expression expression = parser.parseExpression(definition.expression());
            validateAst(definition.ruleId(), ((SpelExpression) expression).getAST());
            return new SpelRuleSet.CompiledRule(definition, expression);
        } catch (ParseException exception) {
            throw new SpelRuleCompilationException("Invalid SpEL rule expression: " + definition.ruleId(), exception);
        }
    }

    private void validateAst(String ruleId, SpelNode node) {
        String nodeType = node.getClass().getSimpleName();
        if (FORBIDDEN_NODE_TYPES.contains(nodeType)) {
            throw new SpelRuleCompilationException("Forbidden SpEL feature in rule " + ruleId + ": " + nodeType);
        }
        for (int index = 0; index < node.getChildCount(); index++) {
            validateAst(ruleId, node.getChild(index));
        }
    }
}
