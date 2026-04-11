package com.exambaby.ppt.service.support;

/**
 * PPT Prompt 构建器
 */
public final class PptPromptBuilder {

    private PptPromptBuilder() {
    }

    public static String buildOutlinePrompt(String content, String language) {
        return "你是一个专业的PPT大纲生成助手。请根据下方的主题和语言，生成一份极其规范的Markdown格式PPT大纲。\n"
                + "【极度重要：如果你不严格遵守以下格式，前端将无法解析，请务必配合】\n"
                + "1. 第一行必须是以 # 开头的总标题。\n"
                + "2. 第二部分必须是 ## 目录，下面列出所有主要章节。\n"
                + "3. 每个章节之间，必须使用 --- 作为独立的分隔行。\n"
                + "4. 每个章节的起始页必须是 ## 数字. 标题 这种带编号的二级标题格式。\n"
                + "5. 具体的幻灯片页面标题必须是 ### 数字.数字 标题 这种三级标题格式。\n"
                + "6. 幻灯片内容必须使用 - 列表项。\n"
                + "7. 严禁输出任何开场白、结束语或提示文字，直接输出 # 开始的内容。\n\n"
                + "【标准输出范例（必须严格模仿此结构）】：\n"
                + "# PPT总标题\n\n"
                + "## 目录\n"
                + "- 章节一名称\n"
                + "- 章节二名称\n\n"
                + "---\n\n"
                + "## 1. 章节一名称\n"
                + "### 1.1 页面标题\n"
                + "- 要点描述内容\n"
                + "- 要点描述内容\n\n"
                + "### 1.2 页面标题\n"
                + "- 内容点\n\n"
                + "---\n\n"
                + "## 2. 章节二名称\n"
                + "...\n\n"
                + "主题：" + nullToEmpty(content) + "\n"
                + "语言：" + nullToEmpty(language);
    }

    public static String buildSlidesPrompt(String content, String language, String style) {
        return "你是一个幻灯片生成专家。请根据以下Markdown大纲，将其转换并扩写为演示文稿需要的结构化JSONL格式。\n"
                + "极度重要/强制格式要求：\n"
                + "1. 绝对不要输出任何\"主标题\"、\"好的,没问题\"之类的解释性对话文本！哪怕一个字也不行！\n"
                + "2. 你的第一次输出必须是 `{` 开始，必须是一行一个合法的独立 JSON 对象。\n"
                + "3. 不要返回JSON数组，不要包括 markdown 的 ```json 标签。\n"
                + "4. 有且仅有五种type: cover, contents, transition, content, end。\n"
                + "5. 必须要有 remark 字段，且内容为详细的、口语化的逐字【演讲稿】，方便演讲者直接照着朗读，而不是简短的提示或备注。\n\n"
                + "风格要求：" + nullToEmpty(style) + "\n"
                + "语言：" + nullToEmpty(language) + "\n\n"
                + "内容大纲：\n" + nullToEmpty(content) + "\n\n"
                + "必须遵循的流式 JSONL (JSON Lines) 示例：\n"
                + "{\"type\": \"cover\", \"data\": {\"title\": \"主标题\", \"text\": \"副标题/汇报人\"}, \"remark\": \"各位领导、同事大家上午好，欢迎参加今天关于XXX的汇报，我是今天的讲师...\"}\n"
                + "{\"type\": \"contents\", \"data\": {\"items\": [\"第一部分：简介\", \"第二部分：详细说明\"]}, \"remark\": \"今天的分享主要分为以上两部分，首先我们会对...进行简要介绍，随后深入探讨...\"}\n"
                + "{\"type\": \"transition\", \"data\": {\"title\": \"第一部分\", \"text\": \"简要介绍...\"}, \"remark\": \"那么，让我们正式进入第一部分的内容。众所周知，近年来的趋势显示...\"}\n"
                + "{\"type\": \"content\", \"data\": {\"title\": \"本页标题\", \"items\": [{\"title\": \"要点1\", \"text\": \"内容描述1...\"}]}, \"remark\": \"在这一页中，我们重点来看图表展示的第一个要点。大家可以看到，因为...所以...\"}\n"
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
