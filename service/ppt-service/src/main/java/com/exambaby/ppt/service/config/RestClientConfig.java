package com.exambaby.ppt.service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP 客户端配置
 */
@Configuration
public class RestClientConfig {

    /**
     * 用于微服务间调用的 RestTemplate（支持负载均衡）
     * 可能会被自动注入权限拦截器
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }

    /**
     * 用于普通业务的 RestTemplate
     */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 专门用于下载外部资源的 RestTemplate
     * 明确 new 一个对象，确保它不会被 Spring 的拦截器污染
     */
    @Bean(name = "imageDownloadTemplate")
    public RestTemplate imageDownloadTemplate() {
        return new RestTemplate();
    }
}
