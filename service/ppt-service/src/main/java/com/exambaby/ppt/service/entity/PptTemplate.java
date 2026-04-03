package com.exambaby.ppt.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * PPT 模板库表
 */
@Data
@TableName("ppt_template")
public class PptTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板名称 (如: 极简极夜黑)
     */
    private String name;

    /**
     * 类别标识 (business, education 等)
     */
    private String category;

    /**
     * 模板预览图 URL
     */
    private String previewUrl;

    /**
     * 模板标准元素定义 JSON 文件所在的 OSS/静态资源 URL
     */
    private String fileUrl;

    /**
     * 是否付费/VIP 模板: 0-免费, 1-VIP
     */
    private Integer isVip;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
