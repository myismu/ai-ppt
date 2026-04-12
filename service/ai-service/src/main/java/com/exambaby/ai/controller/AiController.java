package com.exambaby.ai.controller;

import com.exambaby.ai.adapter.AiImageAdapter;
import com.exambaby.ai.adapter.AiTextAdapter;
import com.exambaby.ai.constant.AiConstants;
import com.exambaby.ai.dto.AiWritingRequest;
import com.exambaby.ai.dto.DocumentToPptRequest;
import com.exambaby.ai.service.DocumentToPptService;
import com.exambaby.ai.vo.AiModelConfigVO;
import com.exambaby.common.core.domain.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * AI 能力提供控制器
 * 聚合了文本处理（流式）和图片生成（单次异步）两大核心能力。
 * 对外屏蔽底层不同模型商（OpenAI/Claude/豆包）的协议差异，提供标准统一的接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiTextAdapter aiTextAdapter;
    private final AiImageAdapter aiImageAdapter;
    private final com.exambaby.ai.service.AiModelConfigService aiModelConfigService;
    private final DocumentToPptService documentToPptService;

    /**
     * 构造函数
     * 基于 Spring 的依赖注入。注意：aiTextAdapter 默认为 DynamicAiTextAdapter (Primary)，
     * aiImageAdapter 则精准注入 doubaoImageAdapter。
     * 
     * @param aiTextAdapter 文本处理适配器
     * @param aiImageAdapter 生图适配器
     * @param aiModelConfigService 模型配置管理服务
     * @param documentToPptService 文档转PPT服务
     */
    public AiController(AiTextAdapter aiTextAdapter,
                        @Qualifier("doubaoImageAdapter") AiImageAdapter aiImageAdapter,
                        com.exambaby.ai.service.AiModelConfigService aiModelConfigService,
                        DocumentToPptService documentToPptService) {
        this.aiTextAdapter = aiTextAdapter;
        this.aiImageAdapter = aiImageAdapter;
        this.aiModelConfigService = aiModelConfigService;
        this.documentToPptService = documentToPptService;
    }

    /**
     * 获取指定场景的可选模型列表
     * 该列表用于前端编辑器中的“模型切换”下拉框，仅返回脱敏后的基本信息（ID、名称、厂商）。
     * 
     * @param sceneType 场景类型 (默认为 text)
     * @return 包裹在 ApiResponse 中的 VO 列表
     */
    @GetMapping("/models")
    public Mono<ApiResponse<List<AiModelConfigVO>>> getAvailableModels(
            @RequestParam(value = "sceneType", defaultValue = AiConstants.Scene.TEXT) String sceneType) {
        return Mono.just(ApiResponse.success(aiModelConfigService.getAvailableModels(sceneType)));
    }

    /**
     * 文本流式处理核心接口 (写作请求)
     * 返回 SSE (Server-Sent Events) 数据流，支持润色、扩写、PPT 大纲生成等长文本任务。
     * 前端可通过 EventSource 或 Fetch 事件流读取数据实现打字机效果。
     * 
     * @param request 写作请求，包含原内容及具体指令
     * @param response HTTP 响应对象，用于设置防缓存及禁用 Nginx 缓冲的 Header
     * @return 文本字符串流
     */
    @PostMapping(value = "/writing", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> writing(@RequestBody AiWritingRequest request, ServerHttpResponse response) {
        log.info("接收到 AI 文本处理请求: 类型={}, 内容长度={}", request.getType(), 
                request.getContent() != null ? request.getContent().length() : 0);
        
        // SSE 关键 Header 设置：确保流数据能实时发送，不被网关或浏览器代理缓存
        response.getHeaders().add("X-Accel-Buffering", "no"); 
        response.getHeaders().setCacheControl("no-cache"); 
        response.getHeaders().setConnection(java.util.Collections.singletonList("keep-alive")); 
        
        return aiTextAdapter.streamWriting(request) 
                .doOnError(e -> log.error("AI 写作流处理异常: {}", e.getMessage()))
                .doOnTerminate(() -> log.info("AI 写作流处理结束"));
    }

    /**
     * 文档转 PPT 大纲 (流式返回)
     * 根据用户上传的长篇文档及需求，利用长上下文大模型（如 mimo-v2-flash）流式生成结构化 PPT 大纲。
     *
     * @param request 包含文件路径及用户 Prompt 的请求
     * @param response HTTP 响应对象
     * @return 文本字符串流 (最终为 JSON 格式)
     */
    @PostMapping(value = "/document-to-ppt")
    public Mono<Void> documentToPpt(@RequestBody DocumentToPptRequest request, ServerHttpResponse response) {
        log.info("接收到文档转 PPT 请求: 文件={}", request.getFileUrl());
        
        response.getHeaders().add("X-Accel-Buffering", "no");
        response.getHeaders().setCacheControl("no-cache");
        response.getHeaders().setConnection(java.util.Collections.singletonList("keep-alive"));
        // 伪装成 SSE 或纯文本流，确保网关不缓冲
        response.getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);

        return response.writeWith(
            documentToPptService.generatePptOutlineStream(request)
                .map(str -> response.bufferFactory().wrap((str + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)))
        )
        .doOnError(e -> log.error("文档转 PPT 流处理异常: {}", e.getMessage()))
        .doOnTerminate(() -> log.info("文档转 PPT 流处理结束"));
    }

    /**
     * AI 生图接口
     * 异步生成图像并返回结果（Base64 字符串或 URL 链接）。
     * 后端会自动将第三方生图 URL 拉取到内网转码，防止前端直接访问时出现防盗链问题。
     * 
     * @param request 图片生成提示词请求
     * @return 包裹了结果数据的统一响应对象
     */
    @PostMapping("/image")
    public Mono<ApiResponse<String>> generateImage(@RequestBody com.exambaby.ai.dto.AiImageRequest request) {
        log.info("接收 AI 生图请求: {}", request.getPrompt());
        return aiImageAdapter.generateImage(request)
                .map(ApiResponse::success);
    }
}
