package com.exambaby.ai.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 提问词（Prompt）模板枚举
 * 预置了不同业务场景下的系统指令，确保大模型生成的 PPT 内容符合逻辑与结构要求。
 */
@Getter
@AllArgsConstructor
public enum AiPromptEnum {

    /**
     * 长篇文档转 PPT 模板
     */
    DOCUMENT_TO_PPT("document_to_ppt", "你是一个资深的 PPT 策划专家和学术内容提炼师。\n" +
            "请你根据我提供的【参考资料】(如论文全文) 和【用户具体需求】，为其规划并生成一份高质量的、逻辑清晰的 PPT 大纲。\n" +
            "\n" +
            "【执行步骤与要求】\n" +
            "1. 深度阅读参考资料，准确把握其核心论点、研究背景、方法论、实验数据和结论。\n" +
            "2. 结合用户的具体需求，合理规划 PPT 的整体结构（包含：封面、目录页、过渡页、图文内容页、总结致谢页）。\n" +
            "3. 提取资料中的关键信息，将其转化为适合在 PPT 上展示的精炼文案。禁止使用大段长篇大论，必须使用短句或要点式(Bullet Points)进行提炼。\n" +
            "4. 你必须严格使用 Markdown 格式返回大纲，不要输出 JSON。使用一级标题（#）表示 PPT 主标题，二级标题（##）表示章节标题，无序列表（-）表示具体的要点内容。"),

    /**
     * PPT 大纲生成模板
     */
    PPT_OUTLINE("ppt_outline", "你是一个资深的 PPT 架构师。请根据用户提供的主题或内容，生成一份逻辑严密、结构清晰的 PPT 大纲。要求包含：标题、副标题、以及 5-10 个章节详情。"),

    /**
     * 文本润色模板
     */
    POLISH("polish", "你是一个文字编辑专家。请对用户输入的文本进行润色，在保持原意的前提下，显著提升其文学性、专业性和流畅度。"),

    /**
     * 标题生成模板
     */
    TITLE("title", "你是一个创意策划。请根据内容生成一个极具吸引力、简洁明了的标题。"),

    /**
     * 文本翻译模板 (默认英文)
     */
    TRANSLATE("translate", "你是一个精通多国语言的翻译官。请将用户输入的文本准确、优雅地翻译成目标语言，并根据上下文进行地道化处理。"),

    /**
     * 默认通用助手模板
     */
    DEFAULT("default", "你是一个全能的 AI 助手。");

    private final String type;
    private final String prompt;

    /**
     * 根据业务类型查找对应的 Prompt 模板
     * 
     * @param type 业务类型编码
     * @return 对应的提示词模板，若匹配不到则返回默认的通用助手提示词。
     */
    public static String getPromptByType(String type) {
        for (AiPromptEnum promptEnum : AiPromptEnum.values()) {
            if (promptEnum.getType().equalsIgnoreCase(type)) {
                return promptEnum.getPrompt();
            }
        }
        return DEFAULT.getPrompt();
    }
}
