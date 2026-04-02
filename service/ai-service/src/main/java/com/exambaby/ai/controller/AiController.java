package com.exambaby.ai.controller;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

/**
 * AI 生成控制器
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    /**
     * 生成大纲 (SSE 演示)
     * 
     * @param request 包含用户输入的主题等
     * @return SSE 流
     */
    @PostMapping(value = "/generate-outline", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generateOutline(@RequestBody Map<String, Object> request) {
        String topic = (String) request.getOrDefault("topic", "未设定主题");
        
        // 模拟 LLM 流式输出过程
        String[] mockData = {
            "正在为您生成关于 [" + topic + "] 的 PPT 大纲...",
            "\n1. 介绍",
            "\n2. 核心挑战",
            "\n3. 解决方案",
            "\n4. 结论",
            "\n生成完成！"
        };

        return Flux.interval(Duration.ofMillis(500))
                .take(mockData.length)
                .map(index -> ServerSentEvent.<String>builder()
                        .id(String.valueOf(index))
                        .event("message")
                        .data(mockData[index.intValue()])
                        .build());
    }
}
