package com.exambaby.ai.adapter.impl;

import com.exambaby.ai.adapter.AiTextAdapter;
import com.exambaby.ai.config.AiProperties;
import com.exambaby.ai.dto.AiWritingRequest;
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
 * 小米 MIMO 适配器
 * 实现 Anthropic Messages 协议的文本生成接口
 * 支持流式响应，自动解析 SSE 格式
 */
@Slf4j
@Service("xiaomiMimoAdapter")
public class XiaomiMimoAdapter implements AiTextAdapter {

    private final WebClient webClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public XiaomiMimoAdapter(WebClient.Builder webClientBuilder, AiProperties aiProperties, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * 流式生成文本
     * 根据请求类型构建系统提示词，调用小米 MIMO API 并返回流式响应
     * 
     * 支持的类型：
     * - polish: 文案润色
     * - expand: 内容扩写
     * - summarize: 摘要提取
     * - translate: 翻译
     * 
     * @param request 文本处理请求
     * @return 文本流，每个元素为一个 token
     */
    @Override
    public Flux<String> streamWriting(AiWritingRequest request) {
        log.info("开始调用 AI 接口: {}, 类型: {}", aiProperties.getXiaomi().getBaseUrl(), request.getType());

        String systemPrompt = buildSystemPrompt(request.getType());

        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getXiaomi().getModel());
        body.put("system", systemPrompt);
        body.put("max_tokens", 4096);
        body.put("stream", true);
        
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        
        String content = request.getContent();
        String instruction = request.getInstruction();
        
        if (content == null || content.isBlank()) {
            userMessage.put("content", instruction != null ? instruction : "");
        } else if (instruction == null || instruction.isBlank()) {
            userMessage.put("content", content);
        } else {
            userMessage.put("content", content + "\n\n" + instruction);
        }
        
        body.put("messages", Collections.singletonList(userMessage));

        return webClient.post()
                .uri(aiProperties.getXiaomi().getBaseUrl())
                .header("x-api-key", aiProperties.getXiaomi().getApiKey())
                .header("anthropic-version", aiProperties.getXiaomi().getAnthropicVersion())
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
                    String delta = parseJsonDelta(data);
                    return delta.isEmpty() ? Flux.empty() : Flux.just(delta);
                })
                .doOnTerminate(() -> log.info("AI 流生成结束"))
                .doOnError(e -> log.error("AI 服务调用异常: ", e));
    }

    /**
     * 根据类型构建系统提示词
     * 
     * @param type 处理类型
     * @return 系统提示词
     */
    private String buildSystemPrompt(String type) {
        if (type == null || type.isBlank()) {
            return "你是一个专业的 AI 助手，请根据用户的要求完成任务。";
        }
        return switch (type) {
            case "polish" -> "你是一个文案润色专家，请对用户提供的文本进行优化，使其更加专业、精炼。";
            case "expand" -> "你是一个内容扩写专家，请对用户提供的文本进行扩写，增加细节、论据和逻辑深度。";
            case "summarize" -> "你是一个精炼的摘要专家，请对用户提供的文本进行核心要点提取。";
            default -> "你是一个专业的 AI 助手，请根据用户的要求完成任务。";
        };
    }

    /**
     * 解析 Anthropic 协议中的 JSON 增量
     * 从 SSE 事件的 data 字段中提取文本内容
     * 
     * Anthropic 协议格式：
     * {
     *   "type": "content_block_delta",
     *   "delta": { "text": "实际内容" }
     * }
     * 
     * @param jsonStr JSON 字符串
     * @return 提取的文本内容，无法解析则返回空字符串
     */
    private String parseJsonDelta(String jsonStr) {
        try {
            JsonNode node = objectMapper.readTree(jsonStr);
            if (node.has("type") && "content_block_delta".equals(node.get("type").asText())) {
                return node.get("delta").get("text").asText();
            } else if (node.has("delta") && node.get("delta").has("text")) {
                return node.get("delta").get("text").asText();
            }
        } catch (Exception e) {
            log.warn("无法解析数据帧: {}, 错误: {}", jsonStr, e.getMessage());
        }
        return ""; 
    }
}
