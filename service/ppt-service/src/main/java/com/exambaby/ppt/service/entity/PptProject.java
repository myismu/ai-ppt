package com.exambaby.ppt.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户 PPT 项目主表
 */
@Data
@TableName("ppt_project")
public class PptProject implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目唯一标识，建议雪花算法
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 归属用户 ID (关联用户中心)
     */
    private Long userId;

    /**
     * PPT 标题
     */
    private String title;

    /**
     * 封面图地址
     */
    private String coverUrl;

    /**
     * 关联模板 ID（可为空，表示未使用模板）
     */
    private Long templateId;

    /**
     * 幻灯片 JSON 文件路径 (存储在 file-service，如 /projects/{id}/content.json)
     */
    private String contentUrl;

    /**
     * 主题资源 JSON 文件路径 (存储在 file-service，如 /projects/{id}/theme.json)
     */
    private String themeUrl;

    /**
     * 参考文档（JSON 数组：[{name: '...', url: '...'}]）
     */
    private String refDocuments;

    /**
     * 幻灯片页数（冗余字段，列表展示时无需解析 contentJson）
     */
    private Integer slideCount;

    /**
     * 是否收藏: 0-否, 1-是
     */
    private Integer isStar;

    /**
     * 状态: 0-草稿, 1-已发布, 2-已归档, -1-已删除
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
