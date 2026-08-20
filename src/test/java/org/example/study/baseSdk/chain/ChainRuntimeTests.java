package org.example.study.baseSdk.chain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ChainRuntimeTests {

    @Autowired
    private ChainRuntime chainRuntime;

    @Test
    void executesChainByBeanNamesFromProperties() {
        ChainContext context = new ChainContext("trace-1", "task-1");

        ChainExecuteResult result = chainRuntime.execute("test.detect.chain", context);

        assertThat(result.success()).isTrue();
        assertThat(result.chainName()).isEqualTo("test.detect.chain");
        assertThat(result.nodeResults())
                .extracting(ChainNodeResult::handlerName)
                .containsExactly("test.start.handler", "test.middle.handler", "test.finally.handler");
        assertThat(context.getAttribute("start")).isEqualTo(true);
        assertThat(context.getAttribute("middle")).isEqualTo(true);
        assertThat(context.getAttribute("finally")).isEqualTo(true);
    }
}
