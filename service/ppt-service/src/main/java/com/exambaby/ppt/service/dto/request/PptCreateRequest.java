package com.exambaby.ppt.service.dto.request;

import lombok.Data;
import java.io.Serializable;

/**
 * PPT 创建请求
 */
@Data
public class PptCreateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String title;
}
