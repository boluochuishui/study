package org.example.study.application;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.example.study.baseSdk.storage.DetectionFileStore;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectExecutionMode;
import org.example.study.domain.DetectItem;
import org.example.study.domain.DetectItemRequest;
import org.example.study.domain.DetectRequest;
import org.example.study.exception.DetectRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 先校验请求协议与总量，再保存原数据；样本失败保留为逐项结果。 */
@Component
public class DetectRequestValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DetectRequestValidator.class);
    private final DetectionProperties limits;
    private final DetectionFileStore store;
    private final MediaSampleValidator media;

    public DetectRequestValidator(DetectionProperties limits, DetectionFileStore store, MediaSampleValidator media) {
        this.limits = limits;
        this.store = store;
        this.media = media;
    }

    public List<DetectItem> prepare(DetectRequest request, DetectExecutionMode mode) {
        require(request != null, "Detection request must not be null");
        require(StringUtils.isNotBlank(request.getSceneCode()) && request.getSceneCode().length() <= 128,
                "Scene code must not be blank or too long");
        ContentType type = ContentType.from(request.getContentType());
        require(type != ContentType.MULTIMODAL, "Mixed modality requests are not supported");
        require(mode != DetectExecutionMode.SYNC || type == ContentType.TEXT || type == ContentType.IMAGE,
                "Synchronous media detection is not supported");
        require(request.getItems() != null && !request.getItems().isEmpty(), "Detection items must not be empty");
        int maximum = mode == DetectExecutionMode.SYNC ? 1 : limits.getMaxAsyncItems();
        require(request.getItems().size() <= maximum
                && (type == ContentType.TEXT || type == ContentType.IMAGE || request.getItems().size() == 1),
                "Detection item count limit exceeded");
        require(request.getLanguage() == null || (request.getLanguage().length() <= 64
                && request.getLanguage().matches("[a-zA-Z]{2,8}(-[a-zA-Z0-9]{1,8})*")), "Invalid language tag");
        require(request.getRegion() == null || request.getRegion().matches("[A-Z]{2}|[0-9]{3}"), "Invalid region code");
        require(request.getClientRequestId() == null || request.getClientRequestId().length() <= 128,
                "Client request identifier is too long");
        var ids = new HashSet<String>();
        long textBytes = 0;
        long inlineBytes = 0;
        for (var item : request.getItems()) {
            require(item != null, "Detection item must not be null");
            require(item.getItemId() != null && item.getItemId().matches("[a-zA-Z0-9_-]{1,64}") && ids.add(item.getItemId()),
                    "Item identifiers must be valid and unique");
            if (type == ContentType.TEXT) {
                require(item.getData() == null, "Text items cannot contain binary data");
                require(StringUtils.isNotBlank(item.getText())
                        && item.getText().codePointCount(0, item.getText().length()) <= limits.getMaxTextLength(),
                        "Text is missing or exceeds the length limit");
                textBytes += item.getText().getBytes(StandardCharsets.UTF_8).length;
            } else {
                require(item.getText() == null, "Media items cannot contain inline text");
                if (hasData(item)) {
                    // 在解码和落盘前限制编码体积，避免构造过大的字节数组。
                    long maxEncoded = 4 * ((maximumBytes(type) + 2) / 3);
                    require(item.getData().length() <= maxEncoded, "Encoded sample size limit exceeded");
                    inlineBytes += item.getData().length();
                } else {
                    require(StringUtils.isNotBlank(item.getUrl()), "Media content is missing");
                    validateUrl(item.getUrl());
                }
            }
        }
        require(textBytes <= limits.getMaxTotalTextBytes(), "Total text size limit exceeded");
        require(inlineBytes + textBytes <= limits.getMaxRequestBytes(), "Total sample size limit exceeded");
        var prepared = new ArrayList<DetectItem>();
        for (var item : request.getItems()) {
            prepared.add(prepareItem(item, type));
        }
        return List.copyOf(prepared);
    }

    private DetectItem prepareItem(DetectItemRequest item, ContentType type) {
        String fileId = null;
        String error = "";
        String message = "";
        boolean sample = hasData(item);
        if (sample) {
            try {
                long maximum = maximumBytes(type);
                require(item.getData().length() % 4 == 0
                        && item.getData().matches("[A-Za-z0-9+/]*={0,2}"), "Invalid standard Base64 sample");
                int padding = item.getData().endsWith("==") ? 2 : item.getData().endsWith("=") ? 1 : 0;
                long decodedSize = (long) item.getData().length() / 4 * 3 - padding;
                require(decodedSize > 0 && decodedSize <= maximum, "Decoded sample size limit exceeded");
                byte[] bytes = Base64.getDecoder().decode(item.getData());
                try (var input = new ByteArrayInputStream(bytes)) { fileId = store.save(input, maximum); }
                media.validate(store.resolve(fileId), type);
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("Sample validation failed, itemId={}", item.getItemId(), exception);
                if (fileId != null) {
                    try { store.delete(fileId); }
                    catch (IOException cleanup) { LOGGER.warn("Invalid sample cleanup failed", cleanup); }
                    fileId = null;
                }
                error = "INVALID_SAMPLE";
                message = "Sample is invalid, unsupported, or exceeds its limit";
            }
        }
        // 原数据即便无效也不回退 URL，防止实际检测内容与调用方预期不一致。
        return new DetectItem(item.getItemId(), item.getText(), sample || type == ContentType.TEXT ? null : item.getUrl(),
                fileId, error, message);
    }

    private boolean hasData(DetectItemRequest item) {
        return item.getData() != null && !item.getData().isEmpty();
    }

    private long maximumBytes(ContentType type) {
        return type == ContentType.IMAGE ? limits.getMaxImageBytes() : limits.getMaxMediaBytes();
    }

    private void validateUrl(String url) {
        require(url.length() <= 2048, "Media URL is too long");
        try {
            URI uri = URI.create(url);
            require(("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null, "Invalid media URL");
        } catch (IllegalArgumentException exception) {
            throw new DetectRequestException("INVALID_DETECT_REQUEST", "Invalid media URL");
        }
    }

    private void require(boolean valid, String message) {
        if (!valid) { throw new DetectRequestException("INVALID_DETECT_REQUEST", message); }
    }
}
