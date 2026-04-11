package com.exambaby.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 模型参数配置类
 * 从配置文件加载各 AI 服务的连接参数
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

    /**
     * 小米 MIMO 文本模型配置
     */
    @Data
    public static class XiaomiMimo {
        /**
         * API 密钥
         */
        private String apiKey = "";
        
        /**
         * API 基础地址
         */
        private String baseUrl = "https://api.xiaomi.com/v1/messages";
        
        /**
         * Anthropic 协议版本
         */
        private String anthropicVersion = "2023-06-01";
        
        /**
         * 模型名称
         */
        private String model = "claude-3-5-sonnet-20240620";
    }

    /**
     * 豆包生图模型配置
     */
    @Data
    public static class Doubao {
        /**
         * API 密钥
         */
        private String apiKey = "";
        
        /**
         * API 基础地址
         */
        private String baseUrl = "https://ark.cn-beijing.volces.com/api/v3/images/generations";
        
        /**
         * 模型名称
         */
        private String model = "doubao-image-v2";
    }
}
