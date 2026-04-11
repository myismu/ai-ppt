package com.exambaby.ppt.service.service.impl;

import com.exambaby.common.core.domain.ApiResponse;
import com.exambaby.ppt.service.dto.request.PptImageRequest;
import com.exambaby.ppt.service.feign.AiServiceClient;
import com.exambaby.ppt.service.feign.dto.AiImageFeignRequest;
import com.exambaby.ppt.service.service.PptGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * PPT 生成业务实现类
 * 负责调用 AI 服务生成图片等资源
 */
@Slf4j
@Service
public class PptGenerationServiceImpl implements PptGenerationService {

    private final AiServiceClient aiServiceClient;
    private final org.springframework.web.client.RestTemplate restTemplate;

    public PptGenerationServiceImpl(AiServiceClient aiServiceClient, 
                                  @org.springframework.beans.factory.annotation.Qualifier("imageDownloadTemplate") org.springframework.web.client.RestTemplate restTemplate) {
        this.aiServiceClient = aiServiceClient;
        this.restTemplate = restTemplate;
    }

    /**
     * 生成图片
     * 调用 AI 生图服务，将返回的 URL 图片下载并转换为 Base64 格式
     * 
     * @param request 生图请求，包含提示词
     * @return Base64 编码的图片数据（data:image/png;base64,xxx）或原始 URL
     */
    @Override
    public String generateImage(PptImageRequest request) {
        AiImageFeignRequest aiRequest = new AiImageFeignRequest();
        aiRequest.setPrompt(request.getPrompt());
        ApiResponse<String> response = aiServiceClient.generateImage(aiRequest);
        if (response == null || response.getData() == null || response.getData().isBlank()) {
            log.warn("ai-service 生图返回为空");
            return "";
        }
        
        String urlOrBase64 = response.getData();
        if (urlOrBase64.startsWith("http")) {
            try {
                byte[] imageBytes = restTemplate.getForObject(urlOrBase64, byte[].class);
                if (imageBytes != null) {
                    return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(imageBytes);
                }
            } catch (Exception e) {
                log.error("下载图片失败: {}", urlOrBase64, e);
            }
        }
        return urlOrBase64;
    }

    /**
     * 空值转空字符串
     * 
     * @param content 原始内容
     * @return 非空内容或空字符串
     */
    private String defaultIfBlank(String content) {
        return content == null ? "" : content;
    }
}
