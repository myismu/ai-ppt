package com.exambaby.ppt.service.dto.request;

import lombok.Data;

import java.io.Serializable;

/**
 * AIPPT 生图请求
 */
@Data
public class PptImageRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String prompt;
}
