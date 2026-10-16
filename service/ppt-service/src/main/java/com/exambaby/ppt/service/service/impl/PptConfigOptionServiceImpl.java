package com.exambaby.ppt.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exambaby.ppt.service.entity.PptConfigOption;
import com.exambaby.ppt.service.mapper.PptConfigOptionMapper;
import com.exambaby.ppt.service.service.PptConfigOptionService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PPT 配置选项 Service 实现类
 */
@Service
public class PptConfigOptionServiceImpl extends ServiceImpl<PptConfigOptionMapper, PptConfigOption>
        implements PptConfigOptionService {

    @Override
    public List<PptConfigOption> listByCategory(String category) {
        LambdaQueryWrapper<PptConfigOption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PptConfigOption::getCategory, category)
               .eq(PptConfigOption::getIsActive, 1)
               .orderByDesc(PptConfigOption::getSortOrder);
        return this.list(wrapper);
    }

    @Override
    public Map<String, List<String>> listAllGrouped() {
        // 一次查出所有启用项
        LambdaQueryWrapper<PptConfigOption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PptConfigOption::getIsActive, 1)
               .orderByDesc(PptConfigOption::getSortOrder);
        List<PptConfigOption> all = this.list(wrapper);

        // 按 category 分组，只返回 label 文案列表
        return all.stream()
                .collect(Collectors.groupingBy(
                        PptConfigOption::getCategory,
                        LinkedHashMap::new,
                        Collectors.mapping(PptConfigOption::getLabel, Collectors.toList())
                ));
    }
}
