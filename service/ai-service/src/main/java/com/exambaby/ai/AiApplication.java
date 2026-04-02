package com.exambaby.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * AI 服务启动类
 * 
 * 职责：
 * 1. 接入大语言模型 (LLM) API
 * 2. 提供 SSE 流式响应
 * 3. 实现 Prompt 工程与管理
 * 4. Sentinel 限流保护
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
