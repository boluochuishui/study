package org.example.study.baseSdk.storage;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 验证流式大小限制、路径约束和共享文件保留期限。 */
class DetectionFileStoreTests {
    @TempDir Path directory;

    @Test
    void savesReadableFileAndRejectsTraversal() throws Exception {
        var store = store();
        String id = store.save(new ByteArrayInputStream(new byte[]{1, 2}), 2);
        assertThat(Files.readAllBytes(store.resolve(id))).containsExactly(1, 2);
        assertThatThrownBy(() -> store.resolve("../outside")).isInstanceOf(IllegalArgumentException.class);
        store.delete(id);
        assertThatThrownBy(() -> store.resolve(id)).isInstanceOf(java.io.IOException.class);
    }

    @Test
    void oversizedAndEmptyStreamsLeaveNoFiles() throws Exception {
        var store = store();
        assertThatThrownBy(() -> store.save(new ByteArrayInputStream(new byte[]{1, 2}), 1)).isInstanceOf(java.io.IOException.class);
        assertThatThrownBy(() -> store.save(new ByteArrayInputStream(new byte[0]), 1)).isInstanceOf(java.io.IOException.class);
        try (var files = Files.list(directory)) { assertThat(files.count()).isZero(); }
    }

    @Test
    void cleansOnlyExpiredFiles() throws Exception {
        var store = store();
        String old = store.save(new ByteArrayInputStream(new byte[]{1}), 1);
        String fresh = store.save(new ByteArrayInputStream(new byte[]{2}), 1);
        Files.setLastModifiedTime(store.resolve(old), FileTime.from(Instant.now().minusSeconds(90000)));
        store.cleanup();
        assertThatThrownBy(() -> store.resolve(old)).isInstanceOf(java.io.IOException.class);
        assertThat(store.resolve(fresh)).exists();
    }

    private SharedDetectionFileStore store() throws Exception {
        var properties = new DetectionFileProperties();
        properties.setRoot(directory.toString());
        return new SharedDetectionFileStore(properties);
    }
}
