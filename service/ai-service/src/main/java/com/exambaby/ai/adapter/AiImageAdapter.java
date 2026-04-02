package com.exambaby.ai.adapter;

import reactor.core.publisher.Mono;

/**
 * AI 图片生成适配器接口
 */
public interface AiImageAdapter {
    /**
     * 根据提示词生成图片
     * 
     * @param prompt 提示词
     * @return 图片的 Base64 内容或访问 URL
     */
    Mono<String> generateImage(String prompt);
}
