package com.exambaby.ppt.service.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.io.Serializable;

/**
 * PPT 保存请求
 * 支持保存标题、主题、幻灯片内容和页数
 */
@Data
public class PptSaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 项目 ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * PPT 标题（可选，不传则不更新）
     */
    private String title;

    /**
     * 幻灯片完整 JSON 数据 (Slide[] 序列化)
     */
    private String contentJson;

    /**
     * 主题配置 JSON (SlideTheme 序列化)
     */
    private String themeJson;

    /**
     * 幻灯片页数
     */
    private Integer slideCount;
}
