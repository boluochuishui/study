package org.example.study.detect.text.handler;

import org.example.study.baseSdk.chain.ChainHandler;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.detect.common.handler.DetectHandlerSupport;
import org.example.study.domain.DetectContext;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 生成稳定检测文本及不可逆样本指纹，后续缓存不得使用原始文本。
 */
@Component("detect.text.normalize.handler")
public class TextNormalizeHandler extends DetectHandlerSupport implements ChainHandler<DetectContext> {

    @Override
    public ChainNodeResult handle(DetectContext context, ChainHandlerDefinition handlerDefinition) {
        Instant startedAt = Instant.now();
        String normalizedText = normalize(context.allText());
        context.setNormalizedText(normalizedText);
        context.setContentHash(sha256(normalizedText));
        return pass(handlerDefinition.handlerName(), startedAt);
    }

    static String normalize(String source) {
        String normalized = Normalizer.normalize(source == null ? "" : source, Normalizer.Form.NFKC)
                .replace("\u200B", "")
                .replace("\u200C", "")
                .replace("\u200D", "")
                .replace("\uFEFF", "")
                .toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(normalized.length());
        boolean previousWhitespace = false;
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (Character.isWhitespace(current)) {
                if (!previousWhitespace && !result.isEmpty()) {
                    result.append(' ');
                }
                previousWhitespace = true;
            } else {
                result.append(current);
                previousWhitespace = false;
            }
        }
        return result.toString().trim();
    }

    static String sha256(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}
