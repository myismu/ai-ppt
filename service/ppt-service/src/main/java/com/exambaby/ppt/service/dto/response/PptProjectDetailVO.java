package com.exambaby.ppt.service.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * PPT 项目详情响应对象（包含完整 JSON 数据，用于加载编辑器）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PptProjectDetailVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 项目 ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * PPT 标题
     */
    private String title;

    /**
     * 封面图地址
     */
    private String coverUrl;

    /**
     * 关联模板 ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long templateId;

    /**
     * 幻灯片 JSON 文件路径
     */
    private String contentUrl;

    /**
     * 主题资源 JSON 文件路径
     */
    private String themeUrl;

    /**
     * 幻灯片页数
     */
    private Integer slideCount;

    /**
     * 状态: 0-草稿, 1-已发布, 2-已归档
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
