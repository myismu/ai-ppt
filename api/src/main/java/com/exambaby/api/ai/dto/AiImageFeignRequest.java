package com.exambaby.api.ai.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * ai-service 生图请求（契约 DTO）
 */
@Data
public class AiImageFeignRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 生图提示词 */
    private String prompt;
}
