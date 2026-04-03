package com.exambaby.ppt.service.dto.request;

import lombok.Data;

import java.io.Serializable;

/**
 * AIPPT 大纲生成请求
 */
@Data
public class PptOutlineRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String content;

    private String language;

    private String model;

    private Boolean stream;
}
