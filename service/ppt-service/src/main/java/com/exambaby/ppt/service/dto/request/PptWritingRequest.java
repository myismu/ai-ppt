package com.exambaby.ppt.service.dto.request;

import lombok.Data;

import java.io.Serializable;

/**
 * AIPPT 文案处理请求
 */
@Data
public class PptWritingRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String content;

    private String command;

    private Long modelId;

    private String model;

    private Boolean stream;
}
