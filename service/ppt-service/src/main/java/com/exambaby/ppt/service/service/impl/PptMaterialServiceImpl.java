package com.exambaby.ppt.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exambaby.ppt.service.entity.PptMaterial;
import com.exambaby.ppt.service.mapper.PptMaterialMapper;
import com.exambaby.ppt.service.service.PptMaterialService;
import org.springframework.stereotype.Service;

/**
 * PPT 素材服务实现
 */
@Service
public class PptMaterialServiceImpl extends ServiceImpl<PptMaterialMapper, PptMaterial> implements PptMaterialService {
}
