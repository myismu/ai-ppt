package com.exambaby.ppt.service.feign;

import com.exambaby.common.core.domain.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * file-service Feign 客户端
 * 用于 ppt-service 调用 file-service 的文件上传接口
 *
 * 注意：file-service 的 GlobalResponseHandler 会将返回值包装成 ApiResponse
 * 所以 Feign 返回类型是 ApiResponse<String>
 */
@FeignClient(name = "file-service")
public interface FileServiceClient {

    /**
     * 上传 PPT 项目文本文件（JSON 内容、主题等）
     * @param projectId 项目 ID
     * @param fileName  文件名
     * @param body      包含 content 字段的 Map
     * @return ApiResponse 包装的文件路径
     */
    @PostMapping("/file/upload/project-text")
    ApiResponse<String> uploadProjectText(@RequestParam("projectId") String projectId,
                                          @RequestParam("fileName") String fileName,
                                          @RequestBody Map<String, String> body);

    /**
     * 删除项目所有文件
     * @param projectId 项目 ID
     */
    @DeleteMapping("/file/delete/project/{projectId}")
    ApiResponse<String> deleteProjectFiles(@PathVariable("projectId") String projectId);

    /**
     * 聚合计算多个项目的文件总大小（字节）
     * @param projectIds 项目 ID 列表
     * @return 文件总大小（字节数的字符串）
     */
    @PostMapping("/file/storage/total-size")
    ApiResponse<String> getProjectsTotalSize(@RequestBody java.util.List<String> projectIds);
}
