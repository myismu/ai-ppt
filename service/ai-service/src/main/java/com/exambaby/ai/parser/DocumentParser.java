package com.exambaby.ai.parser;

import java.io.File;

/**
 * 文档解析器通用接口
 */
public interface DocumentParser {

    /**
     * 是否支持解析该文件扩展名
     * @param extension 文件扩展名 (如 pdf, docx)
     * @return boolean
     */
    boolean supports(String extension);

    /**
     * 从文件中提取纯文本内容
     * @param file 目标文件
     * @return 提取出的纯文本
     * @throws Exception 解析异常
     */
    String parse(File file) throws Exception;
}
