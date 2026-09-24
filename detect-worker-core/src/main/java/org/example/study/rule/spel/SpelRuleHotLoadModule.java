package org.example.study.rule.spel;

import java.util.LinkedHashMap;
import java.util.Map;

import org.example.study.baseSdk.hotload.HotLoadModule;
import org.example.study.baseSdk.hotload.HotLoadSource;
import org.example.study.baseSdk.rule.spel.SpelRuleEngine;
import org.example.study.baseSdk.rule.spel.SpelRuleReleaseSnapshot;
import org.example.study.baseSdk.rule.spel.SpelRuleSet;

/** 将数据库发布快照编译为当前服务可直接执行的规则目录。 */
public class SpelRuleHotLoadModule implements HotLoadModule<String, SpelRuleReleaseSnapshot, SpelRuleCatalog> {

    public static final String MODULE_NAME = "spel-rules";

    private final SpelRuleReleaseSource source;
    private final SpelRuleEngine ruleEngine;

    public SpelRuleHotLoadModule(SpelRuleReleaseSource source, SpelRuleEngine ruleEngine) {
        this.source = source;
        this.ruleEngine = ruleEngine;
    }

    @Override
    public String name() {
        return MODULE_NAME;
    }

    @Override
    public HotLoadSource<String, SpelRuleReleaseSnapshot> source() {
        return source;
    }

    @Override
    public SpelRuleCatalog compile(Map<String, SpelRuleReleaseSnapshot> items) {
        Map<String, SpelRuleCatalog.Entry> compiled = new LinkedHashMap<>();
        items.forEach((ruleSetCode, snapshot) -> {
            if (!snapshot.enabled()) {
                return;
            }
            SpelRuleSet ruleSet = ruleEngine.compile(snapshot.rules());
            compiled.put(ruleSetCode, new SpelRuleCatalog.Entry(snapshot.releaseVersion(),
                    snapshot.evaluationMode(), snapshot.errorPolicy(), ruleSet));
        });
        return new SpelRuleCatalog(compiled);
    }
}
