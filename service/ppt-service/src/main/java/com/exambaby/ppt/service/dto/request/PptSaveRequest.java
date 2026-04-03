package com.exambaby.ppt.service.dto.request;

import lombok.Data;
import java.io.Serializable;

/**
 * PPT 保存请求
 */
@Data
public class PptSaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String contentJson;
}
