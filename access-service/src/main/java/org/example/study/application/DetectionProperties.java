package org.example.study.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 检测输入及上传限制，生产环境应根据模型吞吐与存储预算调整。 */
@Component
@ConfigurationProperties("study.detect")
public class DetectionProperties {
    @jakarta.annotation.PostConstruct
    public void validate() {
        if (maxAsyncItems <= 0 || maxAsyncItems > 100
                || maxTextLength <= 0 || maxTotalTextBytes <= 0 || maxImageBytes <= 0 || maxMediaBytes <= 0
                || maxRequestBytes <= 0 || maxRequestBytes > Integer.MAX_VALUE || maxImagePixels <= 0
                || maxImageBytes > maxRequestBytes || maxMediaBytes > maxRequestBytes) {
            throw new IllegalArgumentException("Detection limits must be positive and item limits must not exceed 100");
        }
    }
    private int maxAsyncItems = 100;
    private int maxTextLength = 5000;
    private long maxTotalTextBytes = 262144;
    private long maxImageBytes = 10485760;
    private long maxMediaBytes = 52428800;
    private long maxRequestBytes = 67108864;
    private long maxImagePixels = 20000000;

    public int getMaxAsyncItems() { return maxAsyncItems; }
    public void setMaxAsyncItems(int value) { maxAsyncItems = value; }
    public int getMaxTextLength() { return maxTextLength; }
    public void setMaxTextLength(int value) { maxTextLength = value; }
    public long getMaxTotalTextBytes() { return maxTotalTextBytes; }
    public void setMaxTotalTextBytes(long value) { maxTotalTextBytes = value; }
    public long getMaxImageBytes() { return maxImageBytes; }
    public void setMaxImageBytes(long value) { maxImageBytes = value; }
    public long getMaxMediaBytes() { return maxMediaBytes; }
    public void setMaxMediaBytes(long value) { maxMediaBytes = value; }
    public long getMaxRequestBytes() { return maxRequestBytes; }
    public void setMaxRequestBytes(long value) { maxRequestBytes = value; }
    public long getMaxImagePixels() { return maxImagePixels; }
    public void setMaxImagePixels(long value) { maxImagePixels = value; }
}
