package com.exambaby.ai.adapter;

import com.exambaby.ai.dto.AiWritingRequest;
import reactor.core.publisher.Flux;

/**
 * AI 文本生成适配器接口
 */
public interface AiTextAdapter {
    /**
     * 流式生成文本
     * 
     * @param request 请求内容
     * @return 文本流
     */
    Flux<String> streamWriting(AiWritingRequest request);
}
