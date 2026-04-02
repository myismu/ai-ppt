package com.exambaby.ai.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 文本处理请求 DTO
 */
@Data
public class AiWritingRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 输入的原始文本
     */
    private String content;

    /**
     * 处理类型：润色(polish)、扩写(expand)、摘要(summarize)
     */
    private String type;

    /**
     * 其他自定义指令
     */
    private String instruction;
}
