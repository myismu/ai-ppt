package com.exambaby.ai.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * AI 生图请求 DTO
 */
@Data
public class AiImageRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图片描述提示词
     */
    private String prompt;
}
