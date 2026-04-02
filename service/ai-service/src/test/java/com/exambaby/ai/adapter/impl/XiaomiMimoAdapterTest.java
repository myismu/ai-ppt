package com.exambaby.ai.adapter.impl;

import com.exambaby.ai.config.AiProperties;
import com.exambaby.ai.dto.AiWritingRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;


/**
 * 小米 MIMO 适配器集成测试
 * 用于验证真实的 API 调用和解析逻辑
 */
@SpringBootTest
public class XiaomiMimoAdapterTest {

    @Autowired
    private XiaomiMimoAdapter xiaomiMimoAdapter;

    @Test
    public void testStreamWriting() {
        AiWritingRequest request = new AiWritingRequest();
        request.setContent("你好，请介绍一下你自己。");
        request.setType("polish");

        // 使用 StepVerifier 订阅流并打印结果
        xiaomiMimoAdapter.streamWriting(request)
                .doOnNext(content -> System.out.println("收到响应片段: " + content))
                .as(StepVerifier::create)
                .expectNextCount(1) // 至少收到一个片段
                .thenConsumeWhile(x -> true) // 消费剩下的所有片段
                .verifyComplete();
    }
}
