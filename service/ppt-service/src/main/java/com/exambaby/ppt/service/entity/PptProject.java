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
     * 幻灯片完整 JSON 数据 (或者存 OSS URL)
     */
    private String contentJson;

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
