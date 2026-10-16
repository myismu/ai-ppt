package com.exambaby.ai.adapter.impl;

import com.exambaby.ai.adapter.AiTextAdapter;
import com.exambaby.ai.constant.AiConstants;
import com.exambaby.ai.dto.AiWritingRequest;
import com.exambaby.ai.entity.AiModelConfig;
import com.exambaby.ai.enums.AiApiFormatEnum;
import com.exambaby.ai.service.AiModelConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态文字流式路由处理器
 * 核心设计模式：策略模式（Strategy Pattern）。
 * 作为系统中默认被注入的 AiTextAdapter 对象，它在运行时根据数据库配置（api_format），
 * 动态地将写作请求分发给具体的协议适配器（如 OpenAiAdapter 或 AnthropicAdapter）。
 */
@Slf4j
@Primary
@Service("dynamicAiTextAdapter")
public class DynamicAiTextAdapter implements AiTextAdapter {

    private final AiModelConfigService aiModelConfigService;
    private final Map<String, AiTextAdapter> formatMap = new ConcurrentHashMap<>();

    /**
     * 构造函数：利用 Spring 的集合注入特性
     * 自动扫描并注入系统中所有的 AiTextAdapter 实现类，并根据它们各自声明的 formatCode 进行初始化注册。
     * 
     * @param aiModelConfigService 模型配置服务，用于路由时查询数据库
     * @param adapters 系统中所有已注册的文本适配器列表
     */
    public DynamicAiTextAdapter(AiModelConfigService aiModelConfigService, List<AiTextAdapter> adapters) {
        this.aiModelConfigService = aiModelConfigService;
        // 自动发现所有文本适配器，并根据其支持的格式进行注册
        for (AiTextAdapter adapter : adapters) {
            String format = adapter.getFormat();
            if (format != null) {
                this.formatMap.put(format.toLowerCase(), adapter);
                log.info("AI 文本协议注册成功: [{}] -> {}", format, adapter.getClass().getSimpleName());
            }
        }
    }

    /**
     * 路由处理器本身不参与具体的协议格式注册
     * 避免在构造时的自动扫描中出现递归引用。
     * 
     * @return null
     */
    @Override
    public String getFormat() {
        return null; // 路由处理器本身不参与格式注册
    }

    /**
     * 流式处理
     * @param request 请求内容
     * @return 文案流
     */
    /**
     * 核心路由逻辑：根据模型配置动态分发
     * 
     * @param request 用户的写作请求
     * @return 委托给具体适配器后的异步文本流
     */
    @Override
    public Flux<String> streamWriting(AiWritingRequest request) {
        // 1. 获取该模型在数据库中定义的协议格式 (api_format)
        AiModelConfig aiModelConfig = aiModelConfigService.resolveConfig(AiConstants.Scene.TEXT, request.getModelId());
        String formatCode = aiModelConfig.getApiFormat(); 
        
        // 2. 在内部映射表中尝试匹配对应的适配器实例
        AiTextAdapter targetAdapter = formatMap.get(formatCode != null ? formatCode.toLowerCase() : ""); 
        
        // 3. 兜底策略：如果配置了不支持的格式，由于系统早期主要基于 Anthropic，故默认回退到 Anthropic 协议
        if (targetAdapter == null) {
            targetAdapter = formatMap.get(AiApiFormatEnum.ANTHROPIC.getCode());
        }
        
        if (targetAdapter == null) {
            log.error("未找到任何可用协议适配器，请求格式: {}", formatCode);
            return Flux.error(new RuntimeException("不支持的大模型协议格式: " + formatCode));
        }
        
        log.info("动态路由选用协议: {}, 实际适配器: {}", formatCode, targetAdapter.getClass().getSimpleName());
        // 4. 执行实际的接口调用任务
        return targetAdapter.streamWriting(request);
    }
}
