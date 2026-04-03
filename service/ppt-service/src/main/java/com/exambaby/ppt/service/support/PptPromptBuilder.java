package com.exambaby.ppt.service.support;

/**
 * PPT Prompt 构建器
 */
public final class PptPromptBuilder {

    private PptPromptBuilder() {
    }

    public static String buildOutlinePrompt(String content, String language) {
        return "你是一个专业的PPT大纲生成助手。请根据下方的主题和语言，生成一份结构清晰的Markdown格式PPT大纲。\n"
                + "要求：\n"
                + "- 严格遵循Markdown格式\n"
                + "- 不要输出多余的客套话\n\n"
                + "主题：" + nullToEmpty(content) + "\n"
                + "语言：" + nullToEmpty(language);
    }

    public static String buildSlidesPrompt(String content, String language, String style) {
        return "你是一个幻灯片生成专家。请根据以下Markdown大纲，将其转换并扩写为演示文稿需要的结构化JSONL格式。\n"
                + "极度重要/强制格式要求：\n"
                + "1. 绝对不要输出任何\"主标题\"、\"好的,没问题\"之类的解释性对话文本！哪怕一个字也不行！\n"
                + "2. 你的第一次输出必须是 `{` 开始，必须是一行一个合法的独立 JSON 对象。\n"
                + "3. 不要返回JSON数组，不要包括 markdown 的 ```json 标签。\n"
                + "4. 有且仅有五种type: cover, contents, transition, content, end。\n\n"
                + "风格要求：" + nullToEmpty(style) + "\n"
                + "语言：" + nullToEmpty(language) + "\n\n"
                + "内容大纲：\n" + nullToEmpty(content) + "\n\n"
                + "必须遵循的流式 JSONL (JSON Lines) 示例：\n"
                + "{\"type\": \"cover\", \"data\": {\"title\": \"主标题\", \"text\": \"副标题/汇报人\"}}\n"
                + "{\"type\": \"contents\", \"data\": {\"items\": [\"第一部分：简介\", \"第二部分：详细说明\"]}}\n"
                + "{\"type\": \"transition\", \"data\": {\"title\": \"第一部分\", \"text\": \"简要介绍...\"}}\n"
                + "{\"type\": \"content\", \"data\": {\"title\": \"本页标题\", \"items\": [{\"title\": \"要点1\", \"text\": \"内容描述1...\"}]}}\n"
                + "{\"type\": \"end\"}";
    }

    public static String buildWritingPrompt(String content, String command) {
        String requirement = "请润色这段文本，使其更加专业、连贯。";
        if ("translate".equals(command)) {
            requirement = "请将以下文本翻译为英文，保持专业性。";
        } else if ("summarize".equals(command)) {
            requirement = "请总结以下文本的关键信息。";
        } else if ("expand".equals(command)) {
            requirement = "请详细扩写以下文本。";
        } else if (command != null && !command.isBlank()) {
            requirement = "额外指令：" + command;
        }

        return "你是一个专业的文案润色助手。" + requirement + "\n"
                + "要求：\n"
                + "1. 不要改变原意，保持原本的HTML标签如果存在的话（如有<p>、<span>等请保留原位置）。\n"
                + "2. 直接输出处理后的最终文本，绝对不要输出任何像\"好的\"、\"这段文字的润色结果\"之类的描述废话！哪怕一个字也不行！\n\n"
                + "待处理文本：\n" + nullToEmpty(content);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
