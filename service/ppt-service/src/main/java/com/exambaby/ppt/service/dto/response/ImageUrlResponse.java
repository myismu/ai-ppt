package com.exambaby.ppt.service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 前端兼容的生图响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageUrlResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String url;
}
