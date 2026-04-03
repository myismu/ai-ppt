package com.exambaby.ppt.service.controller;

import com.exambaby.common.core.result.Result;
import com.exambaby.common.core.utils.ThreadLocalContext;
import com.exambaby.ppt.service.dto.request.PptCreateRequest;
import com.exambaby.ppt.service.dto.request.PptGenerateRequest;
import com.exambaby.ppt.service.dto.request.PptImageRequest;
import com.exambaby.ppt.service.dto.request.PptOutlineRequest;
import com.exambaby.ppt.service.dto.request.PptSaveRequest;
import com.exambaby.ppt.service.dto.request.PptWritingRequest;
import com.exambaby.ppt.service.dto.response.ImageUrlResponse;
import com.exambaby.ppt.service.service.PptGenerationService;
import com.exambaby.ppt.service.service.PptProjectService;
import com.exambaby.ppt.service.service.PptStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * PPT 生成接口
 */
@RestController
@RequiredArgsConstructor
public class PptController {

    private final PptGenerationService pptGenerationService;
    private final PptStreamService pptStreamService;
    private final PptProjectService pptProjectService;

    /**
     * 创建 PPT 项目
     */
    @PostMapping("/ppt/create")
    public Result<Long> create(@RequestBody PptCreateRequest request) {
        String userIdStr = ThreadLocalContext.getUserId();
        Long userId = (userIdStr != null && !userIdStr.isBlank()) ? Long.valueOf(userIdStr) : 0L;
        Long projectId = pptProjectService.createProject(userId, request.getTitle());
        return Result.success(projectId);
    }

    /**
     * 保存 PPT 项目内容
     */
    @PostMapping("/ppt/save")
    public Result<Boolean> save(@RequestBody PptSaveRequest request) {
        String userIdStr = ThreadLocalContext.getUserId();
        Long userId = (userIdStr != null && !userIdStr.isBlank()) ? Long.valueOf(userIdStr) : 0L;
        boolean success = pptProjectService.saveProject(userId, request.getId(), request.getContentJson());
        return Result.success(success);
    }

    @PostMapping(value = {"/tools/aippt_outline", "/ppt/outline"}, produces = "text/event-stream")
    public SseEmitter outline(@RequestBody PptOutlineRequest request) {
        return pptStreamService.streamOutline(request);
    }

    @PostMapping(value = {"/tools/aippt", "/ppt/generate"}, produces = "text/event-stream")
    public SseEmitter generate(@RequestBody PptGenerateRequest request) {
        return pptStreamService.streamSlides(request);
    }

    @PostMapping(value = {"/tools/ai_writing", "/ppt/writing"}, produces = "text/event-stream")
    public SseEmitter writing(@RequestBody PptWritingRequest request) {
        return pptStreamService.streamWriting(request);
    }

    @PostMapping({"/tools/img_generate", "/ppt/image"})
    public ImageUrlResponse image(@RequestBody PptImageRequest request) {
        return new ImageUrlResponse(pptGenerationService.generateImage(request));
    }
}
