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
 * AI 能力提供控制器
 * 提供文本处理（流式）和图片生成接口
 * 对接小米 MIMO（文本）和豆包（图片）等 AI 服务
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
     * 文本处理流式接口
     * 支持润色、扩写、摘要、翻译等操作，以 SSE 格式返回流式响应
     * 
     * SSE 格式说明：
     * - 每个 token 作为独立的 SSE 事件返回
     * - 前端需要解析 "data:" 前缀获取实际内容
     * 
     * @param request 文本处理请求，包含内容、类型、指令等
     * @param response HTTP 响应对象，用于设置响应头
     * @return 文本流，每个元素为一个 token
     */
    @PostMapping(value = "/writing", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> writing(@RequestBody AiWritingRequest request, org.springframework.http.server.reactive.ServerHttpResponse response) {
        log.info("接收到 AI 文本处理请求: 类型={}, 内容长度={}", request.getType(), 
                request.getContent() != null ? request.getContent().length() : 0);
        
        response.getHeaders().add("X-Accel-Buffering", "no");
        response.getHeaders().setCacheControl("no-cache");
        response.getHeaders().setConnection(java.util.Collections.singletonList("keep-alive"));
        
        return aiTextAdapter.streamWriting(request)
                .doOnError(e -> log.error("AI 写作流处理异常: {}", e.getMessage()))
                .doOnTerminate(() -> log.info("AI 写作流处理结束"));
    }

    /**
     * AI 生图接口
     * 根据提示词生成图片，返回 Base64 编码的图片数据或图片 URL
     * 
     * @param request 生图请求，包含提示词
     * @return 图片 URL 或 Base64 数据
     */
    @PostMapping("/image")
    public Mono<ApiResponse<String>> generateImage(@RequestBody com.exambaby.ai.dto.AiImageRequest request) {
        log.info("转发 AI 生图请求: {}", request.getPrompt());
        return aiImageAdapter.generateImage(request.getPrompt())
                .map(ApiResponse::success);
    }
}
