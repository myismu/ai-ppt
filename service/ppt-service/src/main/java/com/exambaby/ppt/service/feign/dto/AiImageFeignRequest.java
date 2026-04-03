package com.exambaby.ppt.service.feign.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * ai-service 生图请求
 */
@Data
public class AiImageFeignRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String prompt;
}
