package com.exambaby.ppt.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exambaby.ppt.service.entity.PptConfigOption;

import java.util.List;
import java.util.Map;

/**
 * PPT 配置选项 Service 接口
 */
public interface PptConfigOptionService extends IService<PptConfigOption> {

    /**
     * 按分类获取启用的配置选项列表
     *
     * @param category 分类标识 (language / style / recommend)
     * @return 按 sort_order 降序排列的选项列表
     */
    List<PptConfigOption> listByCategory(String category);

    /**
     * 一次性获取所有分类的配置选项（批量返回，减少请求次数）
     *
     * @return key 为 category，value 为该分类下的选项列表
     */
    Map<String, List<String>> listAllGrouped();
}
