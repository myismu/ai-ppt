package com.exambaby.ai.enums;

import lombok.Getter;

/**
 * AI API 协议格式枚举
 */
@Getter
public enum AiApiFormatEnum {
    OPEN_AI("openai", "OpenAI 兼容协议"),
    ANTHROPIC("anthropic", "Anthropic Messages 协议");

    private final String code;
    private final String description;

    AiApiFormatEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据 code 获取枚举对象
     * @param code 协议代码
     * @return 枚举对象
     */
    public static AiApiFormatEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return ANTHROPIC; // 默认
        }
        for (AiApiFormatEnum format : AiApiFormatEnum.values()) {
            if (format.getCode().equalsIgnoreCase(code)) {
                return format;
            }
        }
        return ANTHROPIC; // 兜底返回 Anthropic
    }
}
