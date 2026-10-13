package org.example.study.baseSdk.sensitive;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 验证常见字段、短值和策略字段的显式脱敏行为。 */
class SensitiveDataMaskerTests {

    @Test
    void masksKnownTypesAndRedactsSecrets() {
        assertThat(SensitiveDataMasker.mask("13812345678", SensitiveType.MOBILE_PHONE))
                .isEqualTo("138****5678");
        assertThat(SensitiveDataMasker.mask("alice@example.com", SensitiveType.EMAIL))
                .contains("@example.com").doesNotContain("alice");
        assertThat(SensitiveDataMasker.mask("110101199001011234", SensitiveType.ID_CARD))
                .doesNotContain("19900101");
        assertThat(SensitiveDataMasker.mask("6222021234567890123", SensitiveType.BANK_CARD))
                .doesNotContain("123456789");
        assertThat(SensitiveDataMasker.mask("secret-value", SensitiveType.SECRET))
                .isEqualTo(SensitiveDataMasker.REDACTED);
    }

    @Test
    void malformedAndShortValuesDoNotRemainUnchanged() {
        assertThat(SensitiveDataMasker.mask("x", SensitiveType.EMAIL))
                .isEqualTo(SensitiveDataMasker.REDACTED);
        assertThat(SensitiveDataMasker.mask("a", SensitiveType.CHINESE_NAME))
                .isEqualTo(SensitiveDataMasker.REDACTED);
        assertThat(SensitiveDataMasker.mask(null, SensitiveType.SECRET)).isNull();
        assertThat(SensitiveDataMasker.mask("", SensitiveType.SECRET)).isEmpty();
    }

    @Test
    void customMaskUsesUnicodeCharactersAndProtectsShortValues() {
        assertThat(SensitiveDataMasker.mask("A😀BC", 1, 1)).isEqualTo("A**C");
        assertThat(SensitiveDataMasker.mask("xy", 2, 2)).isEqualTo("**");
        assertThatThrownBy(() -> SensitiveDataMasker.mask("abc", -1, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fieldPoliciesReturnIndependentImmutableMap() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("phone", "13812345678");
        input.put("token", "secret-value");
        input.put("nestedSecret", Map.of("value", "secret-value"));
        input.put("taskId", "task-1");
        Map<String, Object> masked = SensitiveDataMasker.maskFields(input,
                Map.of("phone", SensitiveType.MOBILE_PHONE, "token", SensitiveType.SECRET,
                        "nestedSecret", SensitiveType.SECRET));

        assertThat(masked).containsEntry("phone", "138****5678")
                .containsEntry("token", SensitiveDataMasker.REDACTED)
                .containsEntry("nestedSecret", SensitiveDataMasker.REDACTED)
                .containsEntry("taskId", "task-1");
        assertThat(input.get("token")).isEqualTo("secret-value");
        assertThatThrownBy(() -> masked.put("other", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void nullSensitivePolicyIsRejected() {
        Map<String, SensitiveType> policies = new LinkedHashMap<>();
        policies.put("token", null);
        assertThatThrownBy(() -> SensitiveDataMasker.maskFields(Map.of("token", "secret"), policies))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
