package com.exambaby.ppt.service.controller;

import com.exambaby.common.core.domain.ApiResponse;
import com.exambaby.common.core.result.Result;
import com.exambaby.common.core.utils.ThreadLocalContext;
import com.exambaby.ppt.service.dto.request.PptCreateRequest;
import com.exambaby.ppt.service.dto.request.PptGenerateRequest;
import com.exambaby.ppt.service.dto.request.PptImageRequest;
import com.exambaby.ppt.service.dto.request.PptOutlineRequest;
import com.exambaby.ppt.service.dto.request.PptSaveRequest;
import com.exambaby.ppt.service.dto.request.PptWritingRequest;
import com.exambaby.ppt.service.dto.response.ImageUrlResponse;
import com.exambaby.ppt.service.dto.response.PptIdVO;
import com.exambaby.ppt.service.dto.response.PptProjectDetailVO;
import com.exambaby.ppt.service.dto.response.PptProjectVO;
import com.exambaby.api.file.client.FileServiceClient;
import com.exambaby.ppt.service.service.PptGenerationService;
import com.exambaby.ppt.service.service.PptProjectService;
import com.exambaby.ppt.service.service.PptStreamService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PPT 生成接口控制器
 * 提供 PPT 创建、保存、查询、大纲生成、幻灯片生成、文案处理、图片生成等接口
 */
@RestController
@RequiredArgsConstructor
public class PptController {

    private final PptGenerationService pptGenerationService;
    private final PptStreamService pptStreamService;
    private final PptProjectService pptProjectService;
    private final FileServiceClient fileServiceClient;
    private final com.exambaby.api.ai.client.AiServiceClient aiServiceClient;
    private final com.exambaby.ppt.service.service.PptConfigOptionService pptConfigOptionService;

    // ==================== 项目 CRUD 接口 ====================

    /**
     * 获取创作引擎配置选项（语言、风格、推荐主题）
     * 一次性返回全部分类，减少前端请求次数
     */
    @GetMapping({"/tools/config-options", "/ppt/config-options"})
    public Result<java.util.Map<String, java.util.List<String>>> configOptions() {
        return Result.success(pptConfigOptionService.listAllGrouped());
    }

    /**
     * 获取指定场景的模型列表（代理转发 ai-service）
     */
    @GetMapping({"/tools/models", "/ppt/models"})
    public Result<Object> models(@RequestParam(value = "sceneType", defaultValue = "text") String sceneType) {
        ApiResponse<Object> response = aiServiceClient.getAvailableModels(sceneType);
        if (response != null && response.getCode() == 200) {
            return Result.success(response.getData());
        }
        return Result.error("获取模型列表失败");
    }

    /**
     * 创建 PPT 项目
     * 根据用户 ID 和标题创建新的 PPT 项目，返回项目 ID
     * 
     * @param request 创建请求，包含标题
     * @return 项目 ID
     */
    @PostMapping("/ppt/create")
    public Result<PptIdVO> create(@RequestBody PptCreateRequest request) {
        Long userId = resolveUserId();
        Long projectId = pptProjectService.createProject(userId, request.getTitle());
        return Result.success(new PptIdVO(projectId));
    }

    /**
     * 保存 PPT 项目内容
     * 将编辑后的 PPT 内容 JSON、主题、标题等保存到数据库
     * 
     * @param request 保存请求，包含项目 ID 和各字段
     * @return 是否保存成功
     */
    @PostMapping("/ppt/save")
    public Result<Boolean> save(@RequestBody PptSaveRequest request) {
        Long userId = resolveUserId();
        boolean success = pptProjectService.saveProject(userId, request);
        return Result.success(success);
    }

    /**
     * 获取用户的 PPT 项目列表
     * 支持可选的状态过滤
     * 
     * @param status 状态过滤，-1 表示回收站
     * @return 项目列表
     */
    @GetMapping("/ppt/list")
    public Result<List<PptProjectVO>> list(@RequestParam(required = false) Integer status) {
        Long userId = resolveUserId();
        return Result.success(pptProjectService.listProjects(userId, status));
    }

    /**
     * 获取 PPT 项目详情（用于加载编辑器）
     * 返回含完整 contentJson 和 themeJson 的项目数据
     * 
     * @param id 项目 ID
     * @return 项目详情
     */
    @GetMapping("/ppt/detail/{id}")
    public Result<PptProjectDetailVO> detail(@PathVariable Long id) {
        Long userId = resolveUserId();
        PptProjectDetailVO detail = pptProjectService.getProjectDetail(userId, id);
        if (detail == null) {
            return Result.error("项目不存在或无权限访问");
        }
        return Result.success(detail);
    }

    /**
     * 逻辑删除 PPT 项目（移至回收站）
     * 
     * @param id 项目 ID
     * @return 是否删除成功
     */
    @DeleteMapping("/ppt/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        Long userId = resolveUserId();
        boolean success = pptProjectService.deleteProject(userId, id);
        return Result.success(success);
    }

    /**
     * 恢复 PPT 项目
     * 
     * @param id 项目 ID
     * @return 是否恢复成功
     */
    @PutMapping("/ppt/restore/{id}")
    public Result<Boolean> restore(@PathVariable Long id) {
        Long userId = resolveUserId();
        boolean success = pptProjectService.restoreProject(userId, id);
        return Result.success(success);
    }

    /**
     * 永久物理删除 PPT 项目及其文件
     * 
     * @param id 项目 ID
     * @return 是否彻底删除成功
     */
    @DeleteMapping("/ppt/hardDelete/{id}")
    public Result<Boolean> hardDelete(@PathVariable Long id) {
        Long userId = resolveUserId();
        boolean success = pptProjectService.hardDeleteProject(userId, id);
        return Result.success(success);
    }

    /**
     * 切换项目收藏状态（星标）
     * 
     * @param id 项目 ID
     * @return 切换后的收藏状态 (0 或 1)
     */
    @PutMapping("/ppt/star/{id}")
    public Result<Integer> toggleStar(@PathVariable Long id) {
        Long userId = resolveUserId();
        Integer newStar = pptProjectService.toggleStar(userId, id);
        if (newStar == null) {
            return Result.error("项目不存在或无权限");
        }
        return Result.success(newStar);
    }

    /**
     * 获取用户收藏的项目列表
     * 
     * @return 收藏项目列表
     */
    @GetMapping("/ppt/starred")
    public Result<List<PptProjectVO>> starred() {
        Long userId = resolveUserId();
        return Result.success(pptProjectService.listStarredProjects(userId));
    }

    /**
     * 获取用户云存储空间使用情况
     * 聚合计算用户所有项目关联的文件资源总大小
     * 
     * @return 存储使用信息 Map (usedBytes, usedMB)
     */
    @GetMapping("/ppt/storage")
    public Result<java.util.Map<String, Object>> storage() {
        Long userId = resolveUserId();
        // 获取用户所有项目 ID
        List<PptProjectVO> projects = pptProjectService.listProjects(userId, null);
        List<String> projectIds = projects.stream()
                .map(p -> String.valueOf(p.getId()))
                .collect(java.util.stream.Collectors.toList());

        // 获取项目关联的文件资源总大小
        long totalBytes = 0;
        if (!projectIds.isEmpty()) {
            try {
                var response = fileServiceClient.getProjectsTotalSize(projectIds);// 调用 file-service
                if (response != null && response.getCode() == 200 && response.getData() != null) { // 响应成功
                    totalBytes = Long.parseLong(response.getData());// 转换为字节
                }
            } catch (Exception e) {
                // file-service 不可用时降级为 0
            }
        }
        // 转换为 MB
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("usedBytes", totalBytes);
        result.put("usedMB", Math.round(totalBytes / 1024.0 / 1024.0 * 100.0) / 100.0);
        return Result.success(result);
    }

    // ==================== AI 生成接口 ====================

    /**
     * 生成 PPT 大纲
     * 根据主题和语言生成 Markdown 格式的 PPT 大纲，以流式方式返回
     * 
     * @param request 大纲生成请求，包含主题、语言、模型等信息
     * @param response HTTP 响应对象，用于写入流式数据
     */
    @PostMapping(value = {"/tools/aippt_outline", "/ppt/outline"}, produces = "text/event-stream")
    public void outline(@RequestBody PptOutlineRequest request, HttpServletResponse response) {
        pptStreamService.streamPlainText(request, response);
    }

    /**
     * 生成幻灯片
     * 根据大纲生成结构化的幻灯片 JSONL 数据，以纯 JSON 行格式返回（每行一个 JSON 对象）
     * 
     * @param request 幻灯片生成请求，包含大纲、语言、风格等信息
     * @param response HTTP 响应对象，用于写入流式数据
     */
    @PostMapping(value = {"/tools/aippt", "/ppt/generate"}, produces = "text/event-stream")
    public void generate(@RequestBody PptGenerateRequest request, HttpServletResponse response) {
        pptStreamService.streamSlides(request, response);
    }

    /**
     * 处理文案
     * 支持润色、扩写、翻译、摘要等操作，以流式方式返回处理结果
     * 
     * @param request 文案处理请求，包含内容和处理命令
     * @param response HTTP 响应对象，用于写入流式数据
     */
    @PostMapping(value = {"/tools/ai_writing", "/ppt/writing"}, produces = "text/event-stream")
    public void writing(@RequestBody PptWritingRequest request, HttpServletResponse response) {
        pptStreamService.streamWritingText(request, response);
    }

    /**
     * 生成图片
     * 根据提示词调用 AI 生图接口，返回图片 URL 或 Base64 数据
     * 
     * @param request 生图请求，包含提示词
     * @return 图片 URL 或 Base64 数据
     */
    @PostMapping({"/tools/img_generate", "/ppt/image"})
    public ImageUrlResponse image(@RequestBody PptImageRequest request) {
        return new ImageUrlResponse(pptGenerationService.generateImage(request));
    }

    // ==================== 私有工具方法 ====================

    /**
     * 从 ThreadLocal 中解析当前用户 ID
     */
    private Long resolveUserId() {
        String userIdStr = ThreadLocalContext.getUserId();
        return (userIdStr != null && !userIdStr.isBlank()) ? Long.valueOf(userIdStr) : 0L;
    }
}
