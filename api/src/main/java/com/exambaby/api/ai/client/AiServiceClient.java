package com.exambaby.api.ai.client;

import com.exambaby.api.ai.dto.AiImageFeignRequest;
import com.exambaby.common.core.domain.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * ai-service 契约：AI 服务 Feign 客户端
 *
 * ============================================================
 * 使用说明：
 * 调用方（ppt-service）统一依赖本接口，不要各自手写 Feign 接口。
 * 契约由 ai-service 提供方维护，接口变更时本模块随仓库一起演进，
 * 调用方编译期即可发现不兼容。
 * ============================================================
 */
@FeignClient(name = "ai-service")
public interface AiServiceClient {

    /**
     * AI 生图
     *
     * @param request 生图请求（prompt）
     * @return 生成结果（ApiResponse 包装）
     */
    @PostMapping("/ai/image")
    ApiResponse<String> generateImage(@RequestBody AiImageFeignRequest request);

    /**
     * 查询可用模型列表
     *
     * @param sceneType 场景类型（text / image）
     */
    @GetMapping("/ai/models")
    ApiResponse<Object> getAvailableModels(@RequestParam("sceneType") String sceneType);
}
