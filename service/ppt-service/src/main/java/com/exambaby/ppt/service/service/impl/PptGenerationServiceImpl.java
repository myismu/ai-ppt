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
 * PPT 生成业务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PptGenerationServiceImpl implements PptGenerationService {

    private final AiServiceClient aiServiceClient;
    private final org.springframework.web.client.RestTemplate restTemplate;

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

    private String defaultIfBlank(String content) {
        return content == null ? "" : content;
    }
}
