package com.exambaby.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exambaby.ai.entity.AiModelConfig;
import com.exambaby.ai.vo.AiModelConfigVO;
import java.util.List;

/**
 * AI 模型配置管理 Service 接口
 * 负责大模型节点（如 BaseUrl、ApiKey、协议类型）的增删改查及智能调度逻辑。
 */
public interface AiModelConfigService extends IService<AiModelConfig> {

    /**
     * 获取指定业务场景下的默认模型配置
     * 
     * @param sceneType 业务场景类型（如 text, image）
     * @return 默认的模型配置实体
     */
    AiModelConfig getDefaultConfig(String sceneType);

    /**
     * 智能解析并获取真实的模型配置
     * 如果 modelId 为空或无效，则回退到场景的默认配置。
     * 
     * @param sceneType 业务场景类型
     * @param modelId 前端指定的特定模型 ID（可选）
     * @return 最终生效的模型配置
     */
    AiModelConfig resolveConfig(String sceneType, Long modelId);

    /**
     * 供前端拉取渲染的脱敏列表
     * 
     * @param sceneType 场景类型
     * @return 该场景下启用的可用节点集合
     */
    java.util.List<AiModelConfigVO> getAvailableModels(String sceneType);

}
