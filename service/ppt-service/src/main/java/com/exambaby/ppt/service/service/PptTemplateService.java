package com.exambaby.ppt.service.service;

import com.exambaby.ppt.service.dto.response.PptTemplateVO;

import java.util.List;

/**
 * PPT 模板服务接口
 * 提供 PPT 模板列表查询功能
 */
public interface PptTemplateService {

    /**
     * 获取所有模板列表
     * 
     * @return 模板列表
     */
    List<PptTemplateVO> listTemplates();
}
