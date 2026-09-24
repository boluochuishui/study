package org.example.study.rule.spel;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.example.study.baseSdk.rule.spel.SpelRuleErrorPolicy;
import org.example.study.baseSdk.rule.spel.SpelRuleEvaluationMode;
import org.example.study.baseSdk.rule.spel.SpelRuleSet;

/**
 * 当前服务已生效的规则集目录，是热加载后对检测责任链暴露的只读快照。
 */
public final class SpelRuleCatalog {

    private final Map<String, Entry> entries;

    public SpelRuleCatalog(Map<String, Entry> entries) {
        this.entries = Map.copyOf(entries);
    }

    public Optional<Entry> find(String ruleSetCode) {
        return Optional.ofNullable(entries.get(ruleSetCode));
    }

    public Entry getRequired(String ruleSetCode) {
        Entry entry = entries.get(ruleSetCode);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown SpEL rule set: " + ruleSetCode);
        }
        return entry;
    }

    public Set<String> ruleSetCodes() {
        return entries.keySet();
    }

    /** 单个已编译规则集及其发布元数据。 */
    public record Entry(long releaseVersion, SpelRuleEvaluationMode evaluationMode,
                        SpelRuleErrorPolicy errorPolicy, SpelRuleSet ruleSet) {
    }
}
