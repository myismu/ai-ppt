package com.exambaby.ppt.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exambaby.ppt.service.entity.PptProject;

/**
 * PPT 项目服务接口
 */
public interface PptProjectService extends IService<PptProject> {

    /**
     * 创建 PPT 项目
     * @param userId 用户 ID
     * @param title 标题
     * @return 项目 ID
     */
    Long createProject(Long userId, String title);

    /**
     * 保存 PPT 项目内容
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @param contentJson 内容 JSON
     * @return 是否成功
     */
    boolean saveProject(Long userId, Long projectId, String contentJson);
}
