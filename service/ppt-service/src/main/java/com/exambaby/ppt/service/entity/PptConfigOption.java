package com.exambaby.ppt.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * PPT 创作引擎配置选项表
 * 统一管理语言、风格、推荐主题等可枚举配置
 */
@Data
@TableName("ppt_config_option")
public class PptConfigOption implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置分类: language / style / recommend
     */
    private String category;

    /**
     * 展示文案
     */
    private String label;

    /**
     * 排序权重 (越大越靠前)
     */
    private Integer sortOrder;

    /**
     * 是否启用: 1-是, 0-否
     */
    private Integer isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
