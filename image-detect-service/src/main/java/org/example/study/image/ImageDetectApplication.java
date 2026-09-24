package org.example.study.image;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 图片检测独立服务入口。 */
@EnableDubbo(scanBasePackages = "org.example.study.image.rpc")
@SpringBootApplication(scanBasePackages = "org.example.study")
public class ImageDetectApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImageDetectApplication.class, args);
    }
}
