package com.exambaby.ppt.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * AI 服务流式调用配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ppt.ai")
public class PptAiProperties {

    /**
     * ai-service 基础地址
     */
    private String baseUrl = "http://localhost:8083";
}
