package com.exambaby.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 模型配置数据库实体
 * 对应数据库表 ai_model_config，用于持久化存储大模型的接口地址、密钥及业务参数。
 */
@Data
@TableName(value = "ai_model_config", autoResultMap = true)
public class AiModelConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模型提供商
     * 例如："OpenAI", "Anthropic", "Volcengine"
     */
    private String provider;

    /**
     * 场景类型
     * text (文本生成), image (图片生成)
     */
    private String sceneType;

    /**
     * API 协议格式
     * openai (标准协议), anthropic (消息协议)
     */
    private String apiFormat;

    /**
     * 模型展示名称
     * 例如："DeepSeek-V3", "Claude-3.5-Sonnet"
     */
    private String modelName;

    /**
     * API 基础访问地址
     */
    private String baseUrl;

    /**
     * 访问凭证 (ApiKey)
     */
    private String apiKey;

    /**
     * 扩展配置 (JSON 格式)
     * 用于存储某些模型特有的参数，如 anthropic-version。
     */
    private String extraConfig;

    /**
     * 是否为对应场景的默认模型: 0-否, 1-是
     */
    private Integer isDefault;

    /**
     * 是否启用: 0-禁用, 1-启用
     */
    private Integer isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
