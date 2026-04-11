package com.exambaby.ppt.service.service;

import com.exambaby.ppt.service.dto.request.PptImageRequest;

/**
 * PPT 生成服务接口
 * 提供 AI 生图等功能
 */
public interface PptGenerationService {

    /**
     * 生成图片
     * 
     * @param request 生图请求，包含提示词
     * @return 图片 URL 或 Base64 数据
     */
    String generateImage(PptImageRequest request);
}
