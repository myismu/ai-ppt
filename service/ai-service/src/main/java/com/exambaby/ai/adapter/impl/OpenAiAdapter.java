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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 协议大模型文本适配器实现
 * 支持所有兼容 OpenAI 官方 Chat Completion 接口规范的模型厂商（如 DeepSeek、智谱、Moonshot 等）。
 * 实现了标准的 SSE（Server-Sent Events）流式数据解析逻辑。
 */
@Slf4j
@Service("openAiAdapter")
public class OpenAiAdapter implements AiTextAdapter {

    private final WebClient webClient;
    private final AiModelConfigService aiModelConfigService;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     * @param webClientBuilder 响应式客户端构建器
     * @param aiModelConfigService 配置服务
     * @param objectMapper JSON 序列化工具
     */
    public OpenAiAdapter(WebClient.Builder webClientBuilder, AiModelConfigService aiModelConfigService, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.aiModelConfigService = aiModelConfigService;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取适配器所对应的协议代码
     * @return 固定返回 openai
     */
    @Override
    public String getFormat() {
        return AiApiFormatEnum.OPEN_AI.getCode();
    }

    /**
     *  流式生成文本
     * @param request 请求内容
     * @return 文本流
     */
    /**
     * 流式文本生成主入口
     * 
     * @param request 写作请求
     * @return 增量文本流
     */
    @Override
    public Flux<String> streamWriting(AiWritingRequest request) {
        // 加载当前模型专属的 ApiKey、BaseUrl 等配置
        AiModelConfig aiModelConfig = aiModelConfigService.resolveConfig(AiConstants.Scene.TEXT, request.getModelId());

        log.info("开始调用 AI 接口 (OpenAI格式): {}, 类型: {}, 模型: {}", aiModelConfig.getBaseUrl(), request.getType(), aiModelConfig.getModelName());

        // 获取该业务场景（润色/扩写等）所对应的预设系统 Prompt
        String systemPrompt = AiPromptEnum.getPromptByType(request.getType());

        // 构建标准 OpenAI Chat 请求体
        Map<String, Object> body = new HashMap<>();
        body.put(AiConstants.JsonKey.MODEL, aiModelConfig.getModelName());
        body.put(AiConstants.JsonKey.STREAM, true); // 明确开启 SSE 流式
        
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 装填 System 角色消息：用于约束模型行为
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put(AiConstants.JsonKey.ROLE, AiConstants.Role.SYSTEM);
        systemMessage.put(AiConstants.JsonKey.CONTENT, systemPrompt);
        messages.add(systemMessage);
        
        // 装填 User 角色消息：实际用户输入和待处理文本
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put(AiConstants.JsonKey.ROLE, AiConstants.Role.USER);
        
        String content = request.getContent();
        String instruction = request.getInstruction();
        
        // 智能拼接输入内容与用户自定义指令
        if (content == null || content.isBlank()) {
            userMessage.put(AiConstants.JsonKey.CONTENT, instruction != null ? instruction : "");
        } else if (instruction == null || instruction.isBlank()) {
            userMessage.put(AiConstants.JsonKey.CONTENT, content);
        } else {
            userMessage.put(AiConstants.JsonKey.CONTENT, content + "\n\n" + instruction);
        }
        messages.add(userMessage);
        
        body.put(AiConstants.JsonKey.MESSAGES, messages);

        return webClient.post()
                .uri(aiModelConfig.getBaseUrl())
                // 标准 OpenAI 的 Bearer 认证头
                .header(AiConstants.Header.AUTHORIZATION, AiConstants.Header.BEARER_PREFIX + aiModelConfig.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .flatMap(event -> {
                    String data = event.data();
                    // 识别 OpenAI 协议的流结束标志 [DONE]
                    if (AiConstants.SSE_DONE.equalsIgnoreCase(data)) {
                        return Flux.empty();
                    }
                    if (data == null || data.isEmpty()) {
                        return Flux.empty();
                    }
                    // 解析具体增量包中的 content 片段
                    String delta = parseJsonDelta(data);
                    return delta.isEmpty() ? Flux.empty() : Flux.just(delta);
                })
                .doOnTerminate(() -> log.info("AI 流生成结束"))
                .doOnError(e -> log.error("AI 服务调用异常: ", e));
    }



    /**
     * 解析 OpenAI 格式的 JSON 增量包
     * 剥离外层 JSON 结构（choices[0].delta.content）直接提取文本。
     * 
     * @param jsonStr 原始 SSE 载荷字符串
     * @return 提取出的增量文本，解析失败则返回空串
     */
    private String parseJsonDelta(String jsonStr) {
        try {
            JsonNode node = objectMapper.readTree(jsonStr);
            if (node.has(AiConstants.JsonKey.CHOICES) && node.get(AiConstants.JsonKey.CHOICES).isArray() && node.get(AiConstants.JsonKey.CHOICES).size() > 0) {
                JsonNode delta = node.get(AiConstants.JsonKey.CHOICES).get(0).get(AiConstants.JsonKey.DELTA);
                if (delta != null && delta.has(AiConstants.JsonKey.CONTENT)) {
                    return delta.get(AiConstants.JsonKey.CONTENT).asText();
                }
            }
        } catch (Exception e) {
            log.warn("无法解析数据帧: {}, 错误: {}", jsonStr, e.getMessage());
        }
        return ""; 
    }
}
