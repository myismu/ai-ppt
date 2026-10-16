package com.exambaby.api.ai.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * ai-service 文本处理请求（契约 DTO，用于流式写作接口）
 */
@Data
public class AiWritingFeignRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 模型 id */
    private Long modelId;

    /** 文本内容 */
    private String content;

    /** 类型 */
    private String type;

    /** 指令 */
    private String instruction;
}
