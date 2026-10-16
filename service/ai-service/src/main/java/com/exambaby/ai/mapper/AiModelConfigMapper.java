package com.exambaby.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exambaby.ai.entity.AiModelConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 模型配置 Mapper 接口
 * 继承自 MyBatis-Plus 的 BaseMapper，提供基础的 CRUD 能力。
 */
@Mapper
public interface AiModelConfigMapper extends BaseMapper<AiModelConfig> {
}
