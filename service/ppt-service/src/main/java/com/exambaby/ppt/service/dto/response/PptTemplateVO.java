package com.exambaby.ppt.service.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * PPT 模板响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PptTemplateVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;

    private String category;

    private String coverUrl;

    private String fileUrl;

    private String styleKey;

    private String description;
}
