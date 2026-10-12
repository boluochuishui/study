package org.example.study.baseSdk.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/** 文件引用存储边界，未来可替换为对象存储实现。 */
public interface DetectionFileStore {
    String save(InputStream input, long maxBytes) throws IOException;
    Path resolve(String fileId) throws IOException;
    void delete(String fileId) throws IOException;
}
