package org.example.study.audio;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 音频检测独立服务入口。 */
@EnableDubbo(scanBasePackages = "org.example.study.audio.rpc")
@SpringBootApplication(scanBasePackages = "org.example.study")
public class AudioDetectApplication {
    public static void main(String[] args) {
        SpringApplication.run(AudioDetectApplication.class, args);
    }
}
