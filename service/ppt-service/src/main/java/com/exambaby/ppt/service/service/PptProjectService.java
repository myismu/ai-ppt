package com.exambaby.ppt.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exambaby.ppt.service.dto.request.PptSaveRequest;
import com.exambaby.ppt.service.dto.response.PptProjectDetailVO;
import com.exambaby.ppt.service.dto.response.PptProjectVO;
import com.exambaby.ppt.service.entity.PptProject;

import java.util.List;

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
     * 保存 PPT 项目内容（支持标题、主题、内容的完整保存）
     * @param userId 用户 ID
     * @param request 保存请求
     * @return 是否成功
     */
    boolean saveProject(Long userId, PptSaveRequest request);

    /**
     * 获取项目详情（含完整 JSON，用于加载编辑器）
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @return 项目详情
     */
    PptProjectDetailVO getProjectDetail(Long userId, Long projectId);

    /**
     * 获取用户的项目列表（不含 contentJson 大字段）
     * @param userId 用户 ID
     * @param status 状态过滤 (-1 表示已删除项目，null 或其他表示常规项目)
     * @return 项目列表
     */
    List<PptProjectVO> listProjects(Long userId, Integer status);

    /**
     * 删除项目（逻辑删除，设置 status = -1）
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @return 是否成功
     */
    boolean deleteProject(Long userId, Long projectId);

    /**
     * 恢复项目（设置 status = 0）
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @return 是否成功
     */
    boolean restoreProject(Long userId, Long projectId);

    /**
     * 永久物理删除项目及其静态资源
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @return 是否成功
     */
    boolean hardDeleteProject(Long userId, Long projectId);

    /**
     * 切换项目收藏状态
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @return 切换后的收藏状态 (0 或 1)
     */
    Integer toggleStar(Long userId, Long projectId);

    /**
     * 获取用户收藏的项目列表
     * @param userId 用户 ID
     * @return 收藏项目列表
     */
    List<PptProjectVO> listStarredProjects(Long userId);
}
