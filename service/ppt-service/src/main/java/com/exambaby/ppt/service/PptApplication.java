package com.exambaby.ppt.service;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * PPT 服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.exambaby.ppt.service", "com.exambaby.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.exambaby.api")
@MapperScan("com.exambaby.ppt.service.mapper")
public class PptApplication {

    public static void main(String[] args) {
        SpringApplication.run(PptApplication.class, args);
    }
}
