package com.exambaby.ai.controller;

import com.exambaby.ai.adapter.AiImageAdapter;
import com.exambaby.ai.adapter.AiTextAdapter;
import com.exambaby.ai.dto.AiWritingRequest;
import com.exambaby.common.core.domain.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AI 能力提供接口
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiTextAdapter aiTextAdapter;
    private final AiImageAdapter aiImageAdapter;

    public AiController(@Qualifier("xiaomiMimoAdapter") AiTextAdapter aiTextAdapter,
                        @Qualifier("doubaoImageAdapter") AiImageAdapter aiImageAdapter) {
        this.aiTextAdapter = aiTextAdapter;
        this.aiImageAdapter = aiImageAdapter;
    }

    /**
     * 文本处理流式接口 (SSE)
     * 对接小米 MIMO / Anthropic 协议
     */
    @PostMapping(value = "/writing", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> writing(@RequestBody AiWritingRequest request, org.springframework.http.server.reactive.ServerHttpResponse response) {
        log.info("转发 AI 文本处理请求: {}, 类型: {}", request.getContent(), request.getType());
        
        // 禁用网关和 Nginx 的缓冲
        response.getHeaders().add("X-Accel-Buffering", "no");
        response.getHeaders().setCacheControl("no-cache");
        Flux<String> stringFlux = aiTextAdapter.streamWriting(request);
        log.info("返回 AI 响应: "+stringFlux);
        return stringFlux;
    }

    /**
     * AI 生图接口
     * 对接豆包生图并中转 Base64 解决跨域问题
     */
    @PostMapping("/image")
    public Mono<ApiResponse<String>> generateImage(@RequestBody com.exambaby.ai.dto.AiImageRequest request) {
        log.info("转发 AI 生图请求: {}", request.getPrompt());
        return aiImageAdapter.generateImage(request.getPrompt())
                .map(ApiResponse::success);
    }
}
