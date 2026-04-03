package com.exambaby.ppt.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exambaby.ppt.service.dto.response.PptTemplateVO;
import com.exambaby.ppt.service.entity.PptTemplate;
import com.exambaby.ppt.service.mapper.PptTemplateMapper;
import com.exambaby.ppt.service.service.PptTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PptTemplateServiceImpl extends ServiceImpl<PptTemplateMapper, PptTemplate> implements PptTemplateService {

    @Override
    public List<PptTemplateVO> listTemplates() {
        return this.list().stream().map(template -> {
            PptTemplateVO vo = new PptTemplateVO();
            vo.setId(template.getId());
            vo.setName(template.getName());
            vo.setCategory(template.getCategory());
            vo.setCoverUrl(template.getPreviewUrl());
            // Since the original PptTemplateVO had styleKey and description, I'll map them if needed.
            // But let's keep it simple as the entity has previewUrl and dataJson.
            return vo;
        }).collect(Collectors.toList());
    }
}
