package com.exambaby.ai.adapter;

import com.exambaby.ai.dto.AiWritingRequest;
import reactor.core.publisher.Flux;


/**
 * AI 文本生成适配器接口
 * 定义了 AI 文本流式处理的核心规范，支持 OpenAI 协议和 Anthropic 协议等多种实现。
 */
public interface AiTextAdapter {
    /**
     * 获取当前适配器支持的 API 协议格式
     * 
     * @return 协议格式代码（见 AiApiFormatEnum，如 openai, anthropic）
     */
    String getFormat();

    /**
     * 流式递增生成文本（LLM 核心接口）
     * 通过 WebFlux 的 Flux 返回异步数据流，实现打字机效果。
     * 
     * @param request 文本写作请求对象，包含内容背景、操作指令、模型选择等
     * @return 实时生成的增量文本片段流
     */
    Flux<String> streamWriting(AiWritingRequest request);
}
