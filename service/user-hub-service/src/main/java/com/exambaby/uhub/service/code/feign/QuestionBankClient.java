package com.exambaby.uhub.service.code.feign;

import com.exambaby.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 远程调用题库服务的 Feign 客户端
 */
@FeignClient(name = "question-bank-service")
public interface QuestionBankClient {

    /**
     * 远程新增题库 (支持 userId 和 bankName)
     */
    @PostMapping("/internal/seata/test/addBank")
    Result<Boolean> addBank(@RequestParam("userId") Long userId, @RequestParam("bankName") String bankName);
}
