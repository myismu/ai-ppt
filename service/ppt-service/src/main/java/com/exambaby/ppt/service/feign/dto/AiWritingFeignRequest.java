package com.exambaby.ppt.service.feign.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * ai-service 文本处理请求
 */
@Data
public class AiWritingFeignRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String content;

    private String type;

    private String instruction;
}
