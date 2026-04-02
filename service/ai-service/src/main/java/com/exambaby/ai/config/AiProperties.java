package com.exambaby.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 模型参数配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * 小米 MIMO / Anthropic 文本接口配置
     */
    private XiaomiMimo xiaomi = new XiaomiMimo();

    /**
     * 豆包生图接口配置
     */
    private Doubao doubao = new Doubao();

    @Data
    public static class XiaomiMimo {
        private String apiKey = "";
        private String baseUrl = "https://api.xiaomi.com/v1/messages"; // 示例地址
        private String anthropicVersion = "2023-06-01";
        private String model = "claude-3-5-sonnet-20240620";
    }

    @Data
    public static class Doubao {
        private String apiKey = "";
        private String baseUrl = "https://ark.cn-beijing.volces.com/api/v3/images/generations"; // 示例地址
        private String model = "doubao-image-v2";
    }
}
