package com.exambaby.ppt.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户素材库表
 */
@Data
@TableName("ppt_material")
public class PptMaterial implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 素材 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 素材类型: image, audio, video
     */
    private String type;

    /**
     * 资源实际地址 (OSS)
     */
    private String url;

    /**
     * 文件大小 (字节)
     */
    private Integer size;

    /**
     * 上传时间
     */
    private LocalDateTime createTime;
}
