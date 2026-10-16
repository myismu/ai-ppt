package com.exambaby.ai.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 文本写作请求对象
 * 封装了前端发起润色、扩写、翻译等操作时所需的所有核心参数。
 */
@Data
public class AiWritingRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 目标模型 ID（可选）
     * 如果为空，系统将根据场景自动选择默认模型。
     */
    private Long modelId;

    /**
     * 待处理的原始文本内容
     */
    private String content;

    /**
     * 业务操作类型
     * 参考 AiPromptEnum，如 polish (润色), expand (扩写), ppt_outline (生成大纲)
     */
    private String type;

    /**
     * 其他自定义指令
     */
    private String instruction;
}
