package com.exambaby.ai.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * AI 模型配置脱敏返回视图
 * 用于前端大模型选择列表，不包含敏感的 API Key 和 BaseUrl
 */
@Data
public class AiModelConfigVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 配置 ID (供前端回传调用)
     */
    private Long id;

    /**
     * 供应商名称 (用于分类展示，例如 xiaomi, doubao)
     */
    private String provider;

    /**
     * 模型展示名称
     */
    private String modelName;

    /**
     * 场景类型
     */
    private String sceneType;

    /**
     * 是否为该场景默认配置
     */
    private Integer isDefault;
}
