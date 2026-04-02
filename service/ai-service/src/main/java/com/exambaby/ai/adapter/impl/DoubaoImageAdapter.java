package com.exambaby.ai.adapter.impl;

import com.exambaby.ai.adapter.AiImageAdapter;
import com.exambaby.ai.config.AiProperties;
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
 * 豆包 (火山引擎) 生图适配器
 */
@Slf4j
@Service("doubaoImageAdapter")
public class DoubaoImageAdapter implements AiImageAdapter {

    private final WebClient webClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public DoubaoImageAdapter(WebClient.Builder webClientBuilder, AiProperties aiProperties, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<String> generateImage(String prompt) {
        log.info("调用豆包生图接口，提示词: {}", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getDoubao().getModel());
        body.put("prompt", prompt);

        return webClient.post()
                .uri(aiProperties.getDoubao().getBaseUrl())
                .header("Authorization", "Bearer " + aiProperties.getDoubao().getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(this::extractImageUrlAndDownload)
                .doOnError(e -> log.error("豆包生图出错: ", e));
    }

    /**
     * 提取图片 URL 并下载转为 Base64
     */
    private Mono<String> extractImageUrlAndDownload(String response) {
        try {
            JsonNode node = objectMapper.readTree(response);
            // 豆包 API 返回结构通常在 data[0].url
            String imageUrl = node.get("data").get(0).get("url").asText();
            log.info("提取到生图 URL: {}, 开始下载...", imageUrl);

            // 【关键修复】使用全新的 WebClient 下载，避免携带任何 Authorization 等 Header
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
