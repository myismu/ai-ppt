package com.exambaby.ai.adapter.impl;

import com.exambaby.ai.adapter.AiImageAdapter;
import com.exambaby.ai.constant.AiConstants;
import com.exambaby.ai.entity.AiModelConfig;
import com.exambaby.ai.service.AiModelConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 豆包（火山引擎）生图适配器实现
 * 实现图片生成接口，对接火山引擎图片生成大模型。
 * 支持将 API 返回的图片 URL 自动下载并转换为 Base64 格式返回。
 */
@Slf4j
@Service("doubaoImageAdapter")
public class DoubaoImageAdapter implements AiImageAdapter {

    private final WebClient webClient;
    private final AiModelConfigService aiModelConfigService;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     * @param webClientBuilder WebClient 构建器
     * @param aiModelConfigService 模型配置管理服务
     * @param objectMapper JSON 工具类
     */
    public DoubaoImageAdapter(WebClient.Builder webClientBuilder, AiModelConfigService aiModelConfigService, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.aiModelConfigService = aiModelConfigService;
        this.objectMapper = objectMapper;
    }

    /**
     * 调用 AI 接口生成图像
     * 
     * @param request 生图请求参数
     * @return 包含 Base64 数据或 URL 的 Mono
     */
    @Override
    public Mono<String> generateImage(com.exambaby.ai.dto.AiImageRequest request) {
        AiModelConfig aiModelConfig = aiModelConfigService.resolveConfig(AiConstants.Scene.IMAGE, request.getModelId());
        log.info("调用生图接口: {}, 模型: {}, 提示词: {}", aiModelConfig.getBaseUrl(), aiModelConfig.getModelName(), request.getPrompt());

        Map<String, Object> body = new HashMap<>();
        body.put(AiConstants.JsonKey.MODEL, aiModelConfig.getModelName());
        body.put(AiConstants.JsonKey.PROMPT, request.getPrompt());

        return webClient.post()
                .uri(aiModelConfig.getBaseUrl())
                // 鉴权头部设置
                .header(AiConstants.Header.AUTHORIZATION, AiConstants.Header.BEARER_PREFIX + aiModelConfig.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(this::extractImageUrlAndDownload) // 提取结果并尝试下载转码
                .doOnError(e -> log.error("生图出错: ", e));
    }

    /**
     * 从 API 响应中提取图片 URL 并执行下载转码
     * 
     * @param response 接口原始 JSON 字符串
     * @return Base64 字符串（转换失败则返回原始 URL）
     */
    private Mono<String> extractImageUrlAndDownload(String response) {
        try {
            JsonNode node = objectMapper.readTree(response);
            // 按照豆包 API 规约提取第一个生成的图片链接
            String imageUrl = node.get(AiConstants.JsonKey.DATA).get(0).get(AiConstants.JsonKey.URL).asText();
            log.info("提取到生图 URL: {}, 开始下载...", imageUrl);

            // 下载图片并转换为 Base64，解决跨域或内网访问限制问题
            return WebClient.create().get()
                    .uri(imageUrl)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .map(bytes -> "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes))
                    .onErrorResume(e -> {
                        log.warn("图片下载并转 Base64 失败，返回原始 URL. 错误: {}", e.getMessage());
                        return Mono.just(imageUrl);
                    });

        } catch (Exception e) {
            log.error("解析生图响应失败: {}", e.getMessage());
            return Mono.error(e);
        }
    }
}
