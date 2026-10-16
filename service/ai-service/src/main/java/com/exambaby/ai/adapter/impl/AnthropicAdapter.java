package com.exambaby.ai.adapter.impl;

import com.exambaby.ai.adapter.AiTextAdapter;
import com.exambaby.ai.constant.AiConstants;
import com.exambaby.ai.dto.AiWritingRequest;
import com.exambaby.ai.entity.AiModelConfig;
import com.exambaby.ai.enums.AiApiFormatEnum;
import com.exambaby.ai.enums.AiPromptEnum;
import com.exambaby.ai.service.AiModelConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Anthropic Messages 协议适配器实现
 * 专门用于对接 Claude 系列模型，通过 HTTP Post 请求发送符合 Anthropic 规约的 JSON。
 * 支持 WebFlux 响应流处理和自定义 API 版本控制。
 */
@Slf4j
@Service("anthropicAdapter")
public class AnthropicAdapter implements AiTextAdapter {

    private final WebClient webClient;
    private final AiModelConfigService aiModelConfigService;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     * @param webClientBuilder Spring WebClient 辅助构建类
     * @param aiModelConfigService 模型配置管理服务
     * @param objectMapper JSON 序列化工具
     */
    public AnthropicAdapter(WebClient.Builder webClientBuilder, AiModelConfigService aiModelConfigService, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.aiModelConfigService = aiModelConfigService;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取适配器所对应的 API 协议代码
     * @return 固定返回 anthropic
     */
    @Override
    public String getFormat() {
        return AiApiFormatEnum.ANTHROPIC.getCode();
    }

    /**
     * 异步流式生成文本
     * 包含完整的配置加载、请求体构建、SSE 数据解析全流程。
     * 
     * @param request 文本生成请求
     * @return 增量文本流片段
     */
    @Override
    public Flux<String> streamWriting(AiWritingRequest request) {
        AiModelConfig aiModelConfig = aiModelConfigService.resolveConfig(AiConstants.Scene.TEXT, request.getModelId());
        String anthropicVersion = AiConstants.Header.ANTHROPIC_VERSION_DEFAULT; // 默认值
        try {
            // 解析扩展配置中是否有自定义的版本号
            if (aiModelConfig.getExtraConfig() != null && !aiModelConfig.getExtraConfig().isBlank()) {
                JsonNode jsonNode = objectMapper.readTree(aiModelConfig.getExtraConfig());
                if (jsonNode.has(AiConstants.JsonKey.ANTHROPIC_VERSION_KEY)) {
                    anthropicVersion = jsonNode.get(AiConstants.JsonKey.ANTHROPIC_VERSION_KEY).asText();// 默认值
                }
            }
        } catch (Exception e) {
            log.warn("解析 extra_config 失败，使用默认 authropicVersion: {}", e.getMessage());
        }

        log.info("开始调用 AI 接口: {}, 类型: {}, 模型: {}", aiModelConfig.getBaseUrl(), request.getType(), aiModelConfig.getModelName());

        // 根据业务操作类型检索预置的背景提示词
        String systemPrompt = AiPromptEnum.getPromptByType(request.getType());

        // 构建 Anthropic 官方 Messages 接口请求内容
        Map<String, Object> body = new HashMap<>();
        body.put(AiConstants.JsonKey.MODEL, aiModelConfig.getModelName());
        body.put(AiConstants.JsonKey.SYSTEM, systemPrompt);
        body.put(AiConstants.JsonKey.MAX_TOKENS, 4096);
        body.put(AiConstants.JsonKey.STREAM, true); // 强制开启流式响应
        
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put(AiConstants.JsonKey.ROLE, AiConstants.Role.USER);
        
        String content = request.getContent();
        String instruction = request.getInstruction();
        
        // 拼接用户输入的内容与额外补充指令
        if (content == null || content.isBlank()) {
            userMessage.put(AiConstants.JsonKey.CONTENT, instruction != null ? instruction : "");
        } else if (instruction == null || instruction.isBlank()) {
            userMessage.put(AiConstants.JsonKey.CONTENT, content);
        } else {
            userMessage.put(AiConstants.JsonKey.CONTENT, content + "\n\n" + instruction);
        }
        
        body.put(AiConstants.JsonKey.MESSAGES, Collections.singletonList(userMessage));

        return webClient.post()
                .uri(aiModelConfig.getBaseUrl())
                // Anthropic 强制要求的请求头认证与版本声明
                .header(AiConstants.Header.X_API_KEY, aiModelConfig.getApiKey())
                .header(AiConstants.Header.ANTHROPIC_VERSION, anthropicVersion)// 设置Anthropic API版本
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .flatMap(event -> {
                    String data = event.data();
                    if (data == null || data.isEmpty()) {
                        return Flux.empty();
                    }
                    // 解析 SSE 数据包，提取增量内容。
                    // 注意：Anthropic 的 data 可能包含 content_block_delta 等特殊结构。
                    return parseAnthropicDelta(data);
                })
                .doOnTerminate(() -> log.info("Anthropic AI 流生成结束"))
                .doOnError(e -> log.error("Anthropic AI 服务调用异常: ", e));
    }

    /**
     * 解析 Anthropic SSE 数据帧
     * 从 json 数据中提取 content_block_delta 或其它增量字段。
     * 
     * @param jsonStr SSE 原始 JSON
     * @return 增量文本流
     */
    private Flux<String> parseAnthropicDelta(String jsonStr) {
        try {
            JsonNode node = objectMapper.readTree(jsonStr);
            // 处理 content_block_delta 类型的数据帧
            if (node.has("delta") && node.get("delta").has("text")) {
                return Flux.just(node.get("delta").get("text").asText());
            }
        } catch (Exception e) {
            log.warn("无法解析 Anthropic 数据帧: {}, 错误: {}", jsonStr, e.getMessage());
        }
        return Flux.empty();
    }
}
