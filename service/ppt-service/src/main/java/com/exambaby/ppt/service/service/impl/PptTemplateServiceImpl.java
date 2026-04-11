package com.exambaby.ppt.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exambaby.ppt.service.dto.response.PptTemplateVO;
import com.exambaby.ppt.service.entity.PptTemplate;
import com.exambaby.ppt.service.mapper.PptTemplateMapper;
import com.exambaby.ppt.service.service.PptTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PPT 模板服务实现类
 * 提供 PPT 模板列表查询功能
 */
@Service
public class PptTemplateServiceImpl extends ServiceImpl<PptTemplateMapper, PptTemplate> implements PptTemplateService {

    /**
     * 获取所有模板列表
     * 从数据库查询所有模板，转换为前端需要的 VO 对象
     * 
     * @return 模板列表
     */
    @Override
    public List<PptTemplateVO> listTemplates() {
        return this.list().stream().map(template -> {
            PptTemplateVO vo = new PptTemplateVO();
            vo.setId(template.getId());
            vo.setName(template.getName());
            vo.setCategory(template.getCategory());
            vo.setCoverUrl(template.getPreviewUrl());
            vo.setFileUrl(template.getFileUrl());
            return vo;
        }).collect(Collectors.toList());
    }
}
