package com.exambaby.ppt.service.feign.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * ai-service 文本处理请求
 */
@Data
public class AiWritingFeignRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long modelId;// 模型id

    private String content;// 文本内容

    private String type;//  类型

    private String instruction;// 指令
}
