package com.exambaby.ai.adapter;

import com.exambaby.ai.dto.AiImageRequest;
import reactor.core.publisher.Mono;

/**
 * AI 图像生成适配器接口
 * 定义了 AI 图像生成的核心规范，不同的模型厂商（如 豆包、OpenAI DALL-E）需实现此接口。
 */
public interface AiImageAdapter {
    /**
     * 根据提示词生成图像
     * 
     * @param request 图像生成请求对象，包含提示词、模型ID等信息
     * @return 包含图像数据的 Mono（可能是 Base64 编码或原始 URL）
     */
    Mono<String> generateImage(AiImageRequest request);
}
