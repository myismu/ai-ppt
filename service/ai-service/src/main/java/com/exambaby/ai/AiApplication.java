package com.exambaby.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * AI 能力调度中心启动类
 * 负责大模型适配、流式响应中转以及 AI 资源处理
 */
/**
 * AI 服务启动入口
 * 负责加载 AI 文本解析、图片生成及模型动态路由相关业务。
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.exambaby.ai", "com.exambaby.common"})
public class AiApplication {
    /**
     * 主函数
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
