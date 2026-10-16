package com.exambaby.ai.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * AI 图像生成请求对象
 */
@Data
public class AiImageRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图片描述提示词
     */
    private String prompt;

    /**
     * 目标模型 ID（可选）
     * 如果为空，系统将自动路由到该场景下的默认模型。
     */
    private Long modelId;
}
