package com.exambaby.uhub.service.code.controller;

import com.exambaby.common.core.result.Result;
import com.exambaby.uhub.service.code.entity.Users;
import com.exambaby.uhub.service.code.feign.QuestionBankClient;
import com.exambaby.uhub.service.code.service.UsersService;
import org.apache.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/seata/test")
@RequiredArgsConstructor
public class SeataTestController {

    private final UsersService usersService;
    private final QuestionBankClient questionBankClient;

    @GlobalTransactional(name = "cross-service-rollback", timeoutMills = 10000) // 设置 10 秒超时
    @GetMapping("/rollback")
    public Result<String> testCrossServiceRollback() {
        log.info("【STEP 1】准备执行本地 SQL 插入...");
        
        Users user = new Users();
        user.setUsername("seata_debug_" + System.currentTimeMillis());
        user.setPassword("123456");
        user.setEmail("debug@test.com");
        
        try {
            usersService.save(user);
            log.info("【STEP 2】本地 SQL 执行成功，ID: {}", user.getId());
        } catch (Exception e) {
            log.error("【ERROR】本地 SQL 执行报错: ", e);
            throw e;
        }

        log.info("【STEP 3】准备执行 Feign 远程调用...");
        try {
            Result<Boolean> bankResult = questionBankClient.addBank(user.getId(), "Debug题库");
            log.info("【STEP 4】远程调用返回: {}", bankResult);
        } catch (Exception e) {
            log.error("【ERROR】远程调用报错: ", e);
            throw e;
        }

        log.info("【STEP 5】准备人为抛出异常触发回滚...");
        throw new RuntimeException("DEBUG异常");
    }
}
