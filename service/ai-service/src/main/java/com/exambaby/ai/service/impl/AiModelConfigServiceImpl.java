package com.exambaby.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exambaby.ai.constant.AiConstants;
import com.exambaby.ai.entity.AiModelConfig;
import com.exambaby.ai.mapper.AiModelConfigMapper;
import com.exambaby.ai.service.AiModelConfigService;
import com.exambaby.ai.vo.AiModelConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 模型配置 Service 实现
 * 实现了基于优先级（Default 标志 -> 更新时间）的默认模型智能调度。
 */
@Slf4j
@Service
public class AiModelConfigServiceImpl extends ServiceImpl<AiModelConfigMapper, AiModelConfig> implements AiModelConfigService {

    /**
     * 实现默认配置查找逻辑
     * 1. 优先寻找标记为 is_default=1 且处于启用状态的任务。
     * 2. 如果都没有标记默认，则查找该场景下最新更新的一条启用配置。
     * 
     * @param sceneType 场景 (text/image)
     * @return 最终确定的模型实体
     * @throws RuntimeException 当场景未配置任何可用模型时抛出
     */
    @Override
    public AiModelConfig getDefaultConfig(String sceneType) {
        // 先查询启用的、且设置为默认的
        LambdaQueryWrapper<AiModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelConfig::getSceneType, sceneType)
               .eq(AiModelConfig::getIsActive, 1)
               .eq(AiModelConfig::getIsDefault, 1)
               .orderByDesc(AiModelConfig::getUpdateTime)
               .last("LIMIT 1");
               
        AiModelConfig config = this.getOne(wrapper);
        
        // 如果没有设置默认值，但是存在启用的配置，则取第一条启用的
        if (config == null) {
            LambdaQueryWrapper<AiModelConfig> fallbackWrapper = new LambdaQueryWrapper<>();
            fallbackWrapper.eq(AiModelConfig::getSceneType, sceneType)
                           .eq(AiModelConfig::getIsActive, 1)
                           .orderByDesc(AiModelConfig::getUpdateTime)
                           .last("LIMIT 1");
            config = this.getOne(fallbackWrapper);
        }
        
        if (config == null) {
            log.error("无法找到场景 [{}] 的可用 AI 模型配置！", sceneType);
            throw new RuntimeException("该场景未配置可用的 AI 模型:" + sceneType);
        }
        
        return config;
    }

    /**
     * 在全局配置中解析并校验模型节点
     * 系统会验证 modelId 是否属于当前业务场景，且处于激活状态。
     * 
     * @param sceneType 期望的场景
     * @param modelId 待解析的模型 ID
     * @return 有效的模型配置
     */
    @Override
    public AiModelConfig resolveConfig(String sceneType, Long modelId) {
        if (modelId != null) {
            AiModelConfig config = this.getById(modelId);
            // 校验模型是否存在、是否激活、及其所属场景是否匹配（防止越权或逻辑错误）
            if (config != null && config.getIsActive() == 1 && sceneType.equals(config.getSceneType())) {
                log.info("前端精准指定了 AI 模型节点: [{}] (ID={})", config.getModelName(), modelId);
                return config;
            }
            log.warn("前端传递了非法或已禁用的 AI 模型 ID: {}, 回退为系统默认调度", modelId);
        }
        return getDefaultConfig(sceneType);
    }

    /**
     * 获取指定场景下的可用模型列表
     * 将包含敏感信息的数据库实体映射为精简的 VO 对象，仅保留 ID、名称、厂商及默认标志。
     * 
     * @param sceneType 场景类型
     * @return VO 列表
     */
    @Override
    public List<AiModelConfigVO> getAvailableModels(String sceneType) {
        LambdaQueryWrapper<AiModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelConfig::getSceneType, sceneType)
               .eq(AiModelConfig::getIsActive, 1)
               .orderByDesc(AiModelConfig::getIsDefault)
               .orderByDesc(AiModelConfig::getUpdateTime);
               
        List<AiModelConfig> list = this.list(wrapper);
        return list.stream().map(config -> {
            AiModelConfigVO vo = new AiModelConfigVO();
            vo.setId(config.getId());
            vo.setProvider(config.getProvider());
            vo.setModelName(config.getModelName());
            vo.setSceneType(config.getSceneType());
            vo.setIsDefault(config.getIsDefault());
            return vo;
        }).toList();
    }
}
