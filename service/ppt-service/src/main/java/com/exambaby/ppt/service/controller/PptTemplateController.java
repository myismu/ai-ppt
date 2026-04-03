package com.exambaby.ppt.service.controller;

import com.exambaby.common.core.result.Result;
import com.exambaby.ppt.service.dto.response.PptTemplateVO;
import com.exambaby.ppt.service.service.PptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * PPT 模板接口
 */
@RestController
@RequestMapping("/ppt")
@RequiredArgsConstructor
public class PptTemplateController {

    private final PptTemplateService pptTemplateService;

    @GetMapping("/templates")
    public Result<List<PptTemplateVO>> templates() {
        return Result.success(pptTemplateService.listTemplates());
    }
}
