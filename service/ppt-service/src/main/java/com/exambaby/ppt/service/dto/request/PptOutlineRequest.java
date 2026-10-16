package com.exambaby.ppt.service.dto.request;

import lombok.Data;

import java.io.Serializable;

/**
 * AIPPT 大纲生成请求
 */
@Data
public class PptOutlineRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String content;// 内容

    private String language;// 语言

    private Long modelId;// 模型 ID

    private String model;// 模型

    private Boolean stream;// 流式
}
