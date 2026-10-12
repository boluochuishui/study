package org.example.study.baseSdk.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** 流式写入共享目录，随机引用禁止路径穿越，TTL 清理兼顾异步重投递。 */
public class SharedDetectionFileStore implements DetectionFileStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedDetectionFileStore.class);
    private final Path root;
    private final Duration retention;

    public SharedDetectionFileStore(DetectionFileProperties properties) throws IOException {
        root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
        retention = properties.getRetention();
        if (retention == null || retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException("File retention must be positive");
        }
        Files.createDirectories(root);
    }

    @Override
    public String save(InputStream input, long maxBytes) throws IOException {
        if (maxBytes <= 0) { throw new IllegalArgumentException("File size limit must be positive"); }
        String id = UUID.randomUUID().toString().replace("-", "");
        Path temporary = Files.createTempFile(root, "upload-", ".part");
        try {
            try (var output = Files.newOutputStream(temporary)) {
                byte[] buffer = new byte[8192];
                long size = 0;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    size += count;
                    if (size > maxBytes) { throw new IOException("File size limit exceeded"); }
                    output.write(buffer, 0, count);
                }
                if (size == 0) { throw new IOException("File must not be empty"); }
            }
            Files.move(temporary, root.resolve(id));
            return id;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public Path resolve(String fileId) throws IOException {
        Path path = path(fileId);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Detection file is missing or expired");
        }
        return path;
    }

    @Override
    public void delete(String fileId) throws IOException { Files.deleteIfExists(path(fileId)); }

    private Path path(String fileId) {
        if (fileId == null || !fileId.matches("[a-f0-9]{32}")) {
            throw new IllegalArgumentException("Invalid detection file identifier");
        }
        return root.resolve(fileId);
    }

    @Scheduled(fixedDelayString = "${base-sdk.detection-files.cleanup-interval-ms:3600000}")
    public void cleanup() {
        Instant cutoff = Instant.now().minus(retention);
        try (var files = Files.list(root)) {
            files.filter(path -> path.getFileName().toString().matches("[a-f0-9]{32}|upload-.*\\.part"))
                    .forEach(path -> {
                        try {
                            if (Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toInstant().isBefore(cutoff)) {
                                Files.deleteIfExists(path);
                            }
                        } catch (IOException exception) { LOGGER.warn("Detection file cleanup failed", exception); }
                    });
        } catch (IOException exception) { LOGGER.warn("Detection file cleanup failed", exception); }
    }
}
