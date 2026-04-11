package com.exambaby.ppt.service.service;

import com.exambaby.ppt.service.dto.request.PptGenerateRequest;
import com.exambaby.ppt.service.dto.request.PptOutlineRequest;
import com.exambaby.ppt.service.dto.request.PptWritingRequest;
import jakarta.servlet.http.HttpServletResponse;
/**
 * PPT 流式服务接口
 * 提供大纲生成、幻灯片生成、文案处理等流式接口
 */
public interface PptStreamService {

    /**
     * 流式生成 PPT 大纲
     * 
     * @param request 大纲生成请求
     * @param response HTTP 响应对象
     */
    void streamPlainText(PptOutlineRequest request, HttpServletResponse response);

    /**
     * 流式处理文案
     * 
     * @param request 文案处理请求
     * @param response HTTP 响应对象
     */
    void streamWritingText(PptWritingRequest request, HttpServletResponse response);

    /**
     * 流式生成幻灯片（JSONL 格式）
     * 每行输出一个完整的 JSON 对象，不带 data: 前缀
     * 
     * @param request 幻灯片生成请求
     * @param response HTTP 响应对象
     */
    void streamSlides(PptGenerateRequest request, HttpServletResponse response);
}
