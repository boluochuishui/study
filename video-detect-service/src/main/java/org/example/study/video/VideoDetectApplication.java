package org.example.study.video;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 视频检测独立服务入口。 */
@EnableDubbo(scanBasePackages = "org.example.study.video.rpc")
@SpringBootApplication(scanBasePackages = "org.example.study")
public class VideoDetectApplication {
    public static void main(String[] args) {
        SpringApplication.run(VideoDetectApplication.class, args);
    }
}
