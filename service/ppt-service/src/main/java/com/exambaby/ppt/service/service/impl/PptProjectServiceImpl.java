package com.exambaby.ppt.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exambaby.ppt.service.entity.PptProject;
import com.exambaby.ppt.service.mapper.PptProjectMapper;
import com.exambaby.ppt.service.service.PptProjectService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * PPT 项目服务实现
 */
@Service
public class PptProjectServiceImpl extends ServiceImpl<PptProjectMapper, PptProject> implements PptProjectService {

    @Override
    public Long createProject(Long userId, String title) {
        PptProject project = new PptProject();
        project.setUserId(userId);
        project.setTitle(title);
        project.setStatus(0); // 0-草稿
        project.setCreateTime(LocalDateTime.now());
        project.setUpdateTime(LocalDateTime.now());
        this.save(project);
        return project.getId();
    }

    @Override
    public boolean saveProject(Long userId, Long projectId, String contentJson) {
        PptProject project = this.getById(projectId);
        if (project == null || !project.getUserId().equals(userId)) {
            return false;
        }
        project.setContentJson(contentJson);
        project.setUpdateTime(LocalDateTime.now());
        return this.updateById(project);
    }
}
