package com.exambaby.ppt.service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 消息包装类
 * 用于统一推送给前端的消息格式
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SseMessage {
    /**
     * 消息类型: status (进度/状态), content (实际内容), error (错误)
     */
    private String type;
    
    /**
     * 消息内容
     */
    private String content;

    public static SseMessage status(String msg) {
        return new SseMessage("status", msg);
    }

    public static SseMessage content(String msg) {
        return new SseMessage("content", msg);
    }

    public static SseMessage error(String msg) {
        return new SseMessage("error", msg);
    }
}
