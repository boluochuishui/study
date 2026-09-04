package org.example.study.domain;

import lombok.Getter;
import lombok.Setter;
import org.example.study.baseSdk.chain.ChainContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Detection-specific context passed across moderation chain handlers.
 */
@Getter
@Setter
public class DetectContext extends ChainContext {

    private final String appId;
    private final String sceneCode;
    private final ContentType contentType;
    private final String text;
    private final String imageUrl;
    private final String audioUrl;
    private final String videoUrl;
    private final List<String> derivedTexts = new ArrayList<>();

    public DetectContext(String traceId, String appId, DetectRequest request) {
        super(traceId, request.taskId());
        this.appId = appId;
        this.sceneCode = request.sceneCode();
        this.contentType = ContentType.from(request.contentType());
        this.text = request.text();
        this.imageUrl = request.imageUrl();
        this.audioUrl = request.audioUrl();
        this.videoUrl = request.videoUrl();
        setLogContext(logContext().withCaller(appId, sceneCode));
    }

    public void addDerivedText(String text) {
        if (text != null && !text.isBlank()) {
            derivedTexts.add(text);
        }
    }

    public List<String> derivedTexts() {
        return List.copyOf(derivedTexts);
    }

    public String allText() {
        StringBuilder builder = new StringBuilder(text == null ? "" : text);
        for (String derivedText : derivedTexts) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(derivedText);
        }
        return builder.toString();
    }
}
