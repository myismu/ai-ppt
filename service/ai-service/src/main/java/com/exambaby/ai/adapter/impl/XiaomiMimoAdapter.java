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
 * 小米 MIMO (Anthropic Messages 协议) 适配器
 * 稳健版：支持标准 SSE 解析与错误追踪
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

    @Override
    public Flux<String> streamWriting(AiWritingRequest request) {
        log.info("开始调用 AI 接口: {}, 类型: {}", aiProperties.getXiaomi().getBaseUrl(), request.getType());

        // 1. 构建系统提示词
        String systemPrompt = "你是一个 PPT 生成专家，擅长将普通文本转换为结构化的、逻辑清晰的内容。";
        if ("polish".equals(request.getType())) {
            systemPrompt += "请对用户提供的文本进行润色，使其更加专业、精炼。";
        } else if ("expand".equals(request.getType())) {
            systemPrompt += "请对用户提供的文本进行扩写，增加细节和论据。";
        } else if ("summarize".equals(request.getType())) {
            systemPrompt += "请对用户提供的文本进行摘要。";
        }

        // 2. 构建请求体 (Anthropic 协议)
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getXiaomi().getModel());
        body.put("system", systemPrompt);
        body.put("max_tokens", 4096);
        body.put("stream", true);
        
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", request.getContent() + (request.getInstruction() != null ? "\n指令: " + request.getInstruction() : ""));
        
        body.put("messages", Collections.singletonList(userMessage));

        // 3. 发起请求并使用 ServerSentEvent 自动解析
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
                    if (data == null || data.isEmpty() || data.contains("message_stop")) {
                        return Flux.empty();
                    }
                    return Flux.just(parseJsonDelta(data));
                })
                .filter(content -> !content.isEmpty())
                .doOnTerminate(() -> log.info("AI 流生成结束"))
                .doOnError(e -> log.error("AI 服务调用异常: ", e));
    }

    /**
     * 解析 Anthropic 协议中的 JSON 增量
     */
    private String parseJsonDelta(String jsonStr) {
        try {
            JsonNode node = objectMapper.readTree(jsonStr);
            // 兼容多种 delta 结构
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
