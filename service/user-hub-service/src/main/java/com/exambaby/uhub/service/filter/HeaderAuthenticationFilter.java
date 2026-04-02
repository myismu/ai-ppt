package com.exambaby.uhub.service.filter;

import com.exambaby.common.core.utils.ThreadLocalContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求头认证过滤器
 * 
 * 功能说明：
 * 从请求头中提取用户信息并存入 ThreadLocal 上下文
 * 
 * 处理流程：
 * 1. 从请求头 X-User-Id 获取用户ID
 * 2. 将用户ID存入 ThreadLocalContext
 * 3. 请求结束后清理 ThreadLocal
 * 
 * 注意：
 * - 使用最高优先级确保最先执行
 * - 继承 OncePerRequestFilter 确保每次请求只执行一次
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    /**
     * 过滤器核心方法
     * 从请求头提取用户信息并设置到线程上下文
     * 
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String systemCode = request.getHeader("X-System-Code");

        if (userId != null && !userId.isEmpty()) {
            ThreadLocalContext.setUserId(userId);
        }

        if (systemCode != null && !systemCode.isEmpty()) {
            ThreadLocalContext.setSystemCode(systemCode);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            ThreadLocalContext.clear();
        }
    }
}
