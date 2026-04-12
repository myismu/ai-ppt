package com.exambaby.ai.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 带有附件的文档转 PPT 请求
 */
@Data
public class DocumentToPptRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户提示词/要求
     */
    private String prompt;

    /**
     * 文件相对路径或文件名
     */
    private String fileUrl;

    /**
     * 模型ID (可选)
     */
    private Long modelId;
}
