package com.exambaby.ppt.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exambaby.ppt.service.dto.request.PptSaveRequest;
import com.exambaby.ppt.service.dto.response.PptProjectDetailVO;
import com.exambaby.ppt.service.dto.response.PptProjectVO;
import com.exambaby.ppt.service.entity.PptProject;
import com.exambaby.ppt.service.feign.FileServiceClient;
import com.exambaby.ppt.service.mapper.PptProjectMapper;
import com.exambaby.ppt.service.service.PptProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PPT 项目服务实现
 * 内容 JSON 存储在 file-service 磁盘文件中，数据库只存文件路径
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class PptProjectServiceImpl extends ServiceImpl<PptProjectMapper, PptProject> implements PptProjectService {

    private final FileServiceClient fileServiceClient;

    @Override
    public Long createProject(Long userId, String title) {
        PptProject project = new PptProject();
        project.setUserId(userId);
        project.setTitle(title);
        project.setStatus(0); // 0-草稿
        project.setSlideCount(0);
        project.setCreateTime(LocalDateTime.now());
        project.setUpdateTime(LocalDateTime.now());
        this.save(project);
        return project.getId();
    }

    @Override
    public boolean saveProject(Long userId, PptSaveRequest request) {
        PptProject project = this.getById(request.getId());
        if (project == null || !project.getUserId().equals(userId)) {
            return false;
        }

        // 按需更新字段，前端传了哪些就更新哪些
        if (request.getTitle() != null) {
            project.setTitle(request.getTitle());
        }

        // contentJson → 上传到 file-service，数据库只存路径
        if (request.getContentJson() != null) {
            try {
                var response = fileServiceClient.uploadProjectText(
                        String.valueOf(project.getId()),
                        "content.json",
                        Map.of("content", request.getContentJson())
                );
                if (response != null && response.getCode() == 200) {
                    project.setContentUrl(response.getData());
                    log.info("项目 {} 内容已保存到文件: {}", project.getId(), response.getData());
                } else {
                    log.error("file-service 返回异常: {}", response);
                    return false;
                }
            } catch (Exception e) {
                log.error("保存内容 JSON 到 file-service 失败: {}", e.getMessage());
                return false;
            }
        }

        // themeJson → 上传到 file-service，数据库只存路径
        if (request.getThemeJson() != null) {
            try {
                var response = fileServiceClient.uploadProjectText(
                        String.valueOf(project.getId()),
                        "theme.json",
                        Map.of("content", request.getThemeJson())
                );
                if (response != null && response.getCode() == 200) {
                    project.setThemeUrl(response.getData());
                    log.info("项目 {} 主题已保存到文件: {}", project.getId(), response.getData());
                } else {
                    log.error("file-service 返回异常: {}", response);
                    return false;
                }
            } catch (Exception e) {
                log.error("保存主题 JSON 到 file-service 失败: {}", e.getMessage());
                return false;
            }
        }
        if (request.getSlideCount() != null) {
            project.setSlideCount(request.getSlideCount());
        }

        project.setUpdateTime(LocalDateTime.now());
        return this.updateById(project);
    }

    @Override
    public PptProjectDetailVO getProjectDetail(Long userId, Long projectId) {
        PptProject project = this.getById(projectId);
        if (project == null || !project.getUserId().equals(userId)) {
            return null;
        }
        // 排除已删除的项目
        if (project.getStatus() != null && project.getStatus() == -1) {
            return null;
        }
        // 方案 B：返回 contentUrl，前端自己去 file-service 拉取 JSON
        return PptProjectDetailVO.builder()
                .id(project.getId())
                .title(project.getTitle())
                .coverUrl(project.getCoverUrl())
                .templateId(project.getTemplateId())
                .contentUrl(project.getContentUrl())
                .themeUrl(project.getThemeUrl())
                .slideCount(project.getSlideCount())
                .status(project.getStatus())
                .createTime(project.getCreateTime())
                .updateTime(project.getUpdateTime())
                .build();
    }

    @Override
    public List<PptProjectVO> listProjects(Long userId, Integer status) {
        // 查询用户的项目，按更新时间倒序
        LambdaQueryWrapper<PptProject> wrapper = new LambdaQueryWrapper<PptProject>()
                .select(PptProject::getId, PptProject::getTitle, PptProject::getCoverUrl,
                        PptProject::getSlideCount, PptProject::getStatus,
                        PptProject::getCreateTime, PptProject::getUpdateTime)
                .eq(PptProject::getUserId, userId);
        
        if (status != null && status == -1) {
            // 查询回收站
            wrapper.eq(PptProject::getStatus, -1);
        } else {
            // 查询常规项目 (排除已删除)
            wrapper.ne(PptProject::getStatus, -1);
        }
        
        wrapper.orderByDesc(PptProject::getUpdateTime);

        return this.list(wrapper).stream().map(project ->
                PptProjectVO.builder()
                        .id(project.getId())
                        .title(project.getTitle())
                        .coverUrl(project.getCoverUrl())
                        .slideCount(project.getSlideCount())
                        .status(project.getStatus())
                        .createTime(project.getCreateTime())
                        .updateTime(project.getUpdateTime())
                        .build()
        ).collect(Collectors.toList());
    }

    @Override
    public boolean deleteProject(Long userId, Long projectId) {
        PptProject project = this.getById(projectId);
        if (project == null || !project.getUserId().equals(userId)) {
            return false;
        }
        // 逻辑删除: 设置 status = -1
        project.setStatus(-1);
        project.setUpdateTime(LocalDateTime.now());
        return this.updateById(project);
    }

    @Override
    public boolean restoreProject(Long userId, Long projectId) {
        PptProject project = this.getById(projectId);
        if (project == null || !project.getUserId().equals(userId)) {
            return false;
        }
        // 恢复项目: 设置 status = 0 (草稿)
        project.setStatus(0);
        project.setUpdateTime(LocalDateTime.now());
        return this.updateById(project);
    }

    @Override
    public boolean hardDeleteProject(Long userId, Long projectId) {
        PptProject project = this.getById(projectId);
        if (project == null || !project.getUserId().equals(userId)) {
            return false;
        }
        
        // 1. 调用 file-service 清理静态资源
        try {
            fileServiceClient.deleteProjectFiles(String.valueOf(projectId));
        } catch (Exception e) {
            log.error("彻底删除项目时清理文件失败, projectId: {}", projectId, e);
            // 继续执行数据库删除，或者根据业务需求抛出异常
        }

        // 2. 物理删除数据库记录
        return this.removeById(projectId);
    }
}
