package org.example.study.application;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.example.study.baseSdk.storage.DetectionFileStore;
import org.example.study.domain.BatchDetectItem;
import org.example.study.domain.BatchDetectRequest;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectExecutionMode;
import org.example.study.exception.DetectRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** 先校验整个协议再保存文件，项目错误归入结果，不阻断有效项目。 */
@Component
public class BatchRequestPreparer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchRequestPreparer.class);
    private final BatchDetectionProperties limits;
    private final DetectionFileStore store;
    private final UploadedMediaValidator media;

    public BatchRequestPreparer(BatchDetectionProperties limits, DetectionFileStore store, UploadedMediaValidator media) {
        this.limits = limits;
        this.store = store;
        this.media = media;
    }

    public List<BatchDetectItem> prepare(BatchDetectRequest request, DetectExecutionMode mode,
                                        Map<String, List<MultipartFile>> files) {
        if (request == null) { throw new DetectRequestException("INVALID_BATCH_REQUEST", "Batch request must not be null"); }
        require(StringUtils.isNotBlank(request.sceneCode()) && request.sceneCode().length() <= 128,
                "Scene code must not be blank or too long");
        ContentType type = ContentType.from(request.contentType());
        require(type != ContentType.MULTIMODAL, "Mixed modality batches are not supported");
        require(mode != DetectExecutionMode.SYNC || type == ContentType.TEXT || type == ContentType.IMAGE,
                "Synchronous media detection is not supported");
        require(request.items() != null && !request.items().isEmpty(), "Batch items must not be empty");
        int maximum = mode == DetectExecutionMode.SYNC ? limits.getMaxSyncItems() : limits.getMaxAsyncItems();
        require(request.items().size() <= maximum && (type == ContentType.TEXT || type == ContentType.IMAGE || request.items().size() == 1),
                "Batch item count limit exceeded");
        require(request.language() == null || (request.language().length() <= 64
                && request.language().matches("[a-zA-Z]{2,8}(-[a-zA-Z0-9]{1,8})*")), "Invalid language tag");
        require(request.region() == null || request.region().matches("[A-Z]{2}|[0-9]{3}"), "Invalid region code");
        require(request.clientRequestId() == null || request.clientRequestId().length() <= 128, "Client request identifier is too long");
        var ids = new HashSet<String>();
        var names = new HashSet<String>();
        long textBytes = 0;
        for (var item : request.items()) {
            if (item == null) { throw new DetectRequestException("INVALID_BATCH_REQUEST", "Batch item must not be null"); }
            require(item.itemId() != null && item.itemId().matches("[a-zA-Z0-9_-]{1,64}") && ids.add(item.itemId()),
                    "Item identifiers must be valid and unique");
            int sources = (item.text() == null ? 0 : 1) + (item.url() == null ? 0 : 1) + (item.filePartName() == null ? 0 : 1);
            require(sources == 1, "Each item must have exactly one content source");
            if (type == ContentType.TEXT) {
                require(StringUtils.isNotBlank(item.text()) && item.text().codePointCount(0, item.text().length()) <= limits.getMaxTextLength(),
                        "Text is missing or exceeds the length limit");
                textBytes += item.text().getBytes(StandardCharsets.UTF_8).length;
            } else {
                require(item.text() == null, "Media items cannot contain inline text");
                if (item.url() != null) { validateUrl(item.url()); }
                else {
                    require(item.filePartName().matches("[a-zA-Z0-9_-]{1,64}") && !item.filePartName().equals("metadata")
                            && names.add(item.filePartName()), "File field references must be valid and unique");
                    require(files.containsKey(item.filePartName()) && files.get(item.filePartName()).size() == 1,
                            "Referenced file field is missing or duplicated");
                }
            }
        }
        require(textBytes <= limits.getMaxTotalTextBytes(), "Total text size limit exceeded");
        require(names.equals(files.keySet()), "Unexpected file fields");
        long total = files.values().stream().flatMap(List::stream).mapToLong(MultipartFile::getSize).sum();
        require(total <= limits.getMaxRequestBytes(), "Total file size limit exceeded");
        var prepared = new ArrayList<BatchDetectItem>();
        for (var item : request.items()) {
            String fileId = null;
            String error = "";
            String message = "";
            if (item.filePartName() != null) {
                var file = files.get(item.filePartName()).getFirst();
                long maximumBytes = type == ContentType.IMAGE ? limits.getMaxImageBytes() : limits.getMaxMediaBytes();
                try {
                    if (file.isEmpty() || file.getSize() > maximumBytes) {
                        throw new java.io.IOException("File size limit exceeded");
                    }
                    try (var input = file.getInputStream()) { fileId = store.save(input, maximumBytes); }
                    media.validate(store.resolve(fileId), type);
                } catch (Exception exception) {
                    LOGGER.warn("Upload item validation failed, itemId={}", item.itemId(), exception);
                    if (fileId != null) {
                        try { store.delete(fileId); }
                        catch (java.io.IOException cleanup) { LOGGER.warn("Invalid upload cleanup failed", cleanup); }
                        fileId = null;
                    }
                    error = "INVALID_UPLOAD";
                    message = "Uploaded file is invalid, unsupported, or exceeds its limit";
                }
            }
            prepared.add(new BatchDetectItem(item.itemId(), item.text(), item.url(), fileId, error, message));
        }
        return List.copyOf(prepared);
    }

    private void validateUrl(String url) {
        require(url.length() <= 2048, "Media URL is too long");
        URI uri = URI.create(url);
        require(("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost() != null && uri.getUserInfo() == null, "Invalid media URL");
    }

    private void require(boolean valid, String message) {
        if (!valid) { throw new DetectRequestException("INVALID_BATCH_REQUEST", message); }
    }
}
