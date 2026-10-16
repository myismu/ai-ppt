package com.exambaby.ppt.service.feign;

import com.exambaby.common.core.domain.ApiResponse;
import com.exambaby.ppt.service.feign.dto.AiImageFeignRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * ai-service 远程调用客户端
 */
@FeignClient(name = "ai-service")
public interface AiServiceClient {

    @PostMapping("/ai/image")
    ApiResponse<String> generateImage(@RequestBody AiImageFeignRequest request);

    @GetMapping("/ai/models")
    ApiResponse<Object> getAvailableModels(@RequestParam("sceneType") String sceneType);
}
