package com.exambaby.ai.constant;

/**
 * AI 服务通用常量定义
 */
public class AiConstants {

    /**
     * 业务场景类型
     */
    public static class Scene {
        public static final String TEXT = "text"; // 文本生成场景
        public static final String IMAGE = "image"; // 图片生成场景
    }

    /**
     * AI 角色定义
     */
    public static class Role {
        public static final String SYSTEM = "system"; // 系统角色
        public static final String USER = "user"; // 用户角色
        public static final String ASSISTANT = "assistant"; // 助手角色
    }

    /**
     * 通用 JSON 字段名
     */
    public static class JsonKey {
        public static final String MODEL = "model"; // 模型名称字段
        public static final String STREAM = "stream"; // 是否流式响应字段
        public static final String MESSAGES = "messages"; // 消息列表字段
        public static final String CONTENT = "content"; // 内容字段
        public static final String ROLE = "role"; // 角色字段
        public static final String SYSTEM = "system"; // 系统提示词字段（Anthropic协议独有）
        public static final String MAX_TOKENS = "max_tokens"; // 最大Token数限制字段
        public static final String PROMPT = "prompt"; // 生图提示词字段
        public static final String DATA = "data"; // 响应数据体字段
        public static final String URL = "url"; // 图片URL字段
        public static final String CHOICES = "choices"; // OpenAI响应选项列表字段
        public static final String DELTA = "delta"; // OpenAI流式增量内容字段
        public static final String ANTHROPIC_VERSION_KEY = "anthropicVersion"; // ExtraConfig中的版本号Key
    }

    /**
     * HTTP Header 常量
     */
    public static class Header {
        public static final String AUTHORIZATION = "Authorization"; // 鉴权Header
        public static final String BEARER_PREFIX = "Bearer "; // Bearer Token前缀
        public static final String X_API_KEY = "x-api-key"; // Anthropic 专用API Key Header
        public static final String ANTHROPIC_VERSION = "anthropic-version"; // Anthropic 专用版本控制Header
        public static final String ANTHROPIC_VERSION_DEFAULT = "2023-06-01"; // Anthropic 默认版本号
    }

    /**
     * SSE 特殊标记
     */
    public static final String SSE_DONE = "[DONE]"; // SSE流结束标记
}
