package org.example.study.text;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** 验证当前模态只依赖自身责任链即可启动。 */
@SpringBootTest(properties = {"dubbo.protocol.port=-1", "dubbo.registry.address=N/A"})
class TextDetectApplicationTests {
    @Test
    void contextLoads() {
    }
}
