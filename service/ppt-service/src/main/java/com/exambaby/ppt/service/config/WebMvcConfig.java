package com.exambaby.ppt.service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置：映射静态资源路径
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目根目录下的 upload 绝对路径
        String uploadPath = System.getProperty("user.dir") + File.separator + "service" + File.separator + "ppt-service" + File.separator + "upload" + File.separator;
        
        // 1. 映射整个 /upload/** 路径（通用）
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + uploadPath);

        // 2. 保留 /templates/** 映射（兼容之前的配置）
        registry.addResourceHandler("/templates/**")
                .addResourceLocations("file:" + uploadPath + "templates" + File.separator);
    }
}
