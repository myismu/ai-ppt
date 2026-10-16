package com.exambaby.ppt.service.service.impl;

import com.exambaby.ppt.service.config.PptAiProperties;
import com.exambaby.ppt.service.dto.request.PptGenerateRequest;
import com.exambaby.ppt.service.dto.request.PptOutlineRequest;
import com.exambaby.ppt.service.dto.request.PptWritingRequest;
import com.exambaby.ppt.service.dto.response.SseMessage;
import com.exambaby.ppt.service.feign.dto.AiWritingFeignRequest;
import com.exambaby.ppt.service.service.PptStreamService;
import com.exambaby.ppt.service.support.PptPromptBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * PPT 流式服务实现类
 * 负责将 AI 服务返回的 SSE 流转换为前端可识别的结构化 JSON 格式
 */
@Slf4j
@Service
public class PptStreamServiceImpl implements PptStreamService {

    private final RestTemplate restTemplate;
    private final PptAiProperties pptAiProperties;
    private final ObjectMapper objectMapper;

    public PptStreamServiceImpl(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate, 
                              PptAiProperties pptAiProperties,
                              ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.pptAiProperties = pptAiProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * 流式生成 PPT 大纲
     */
    @Override
    public void streamPlainText(PptOutlineRequest request, HttpServletResponse response) {
        AiWritingFeignRequest aiRequest = new AiWritingFeignRequest();
        aiRequest.setModelId(request.getModelId());// 模型 ID
        aiRequest.setInstruction(PptPromptBuilder.buildOutlinePrompt(request.getContent(), request.getLanguage()));// 构建 Prompt
        doStreamPlainTextToResponse(aiRequest, response);
    }

    /**
     * 流式处理文案
     */
    @Override
    public void streamWritingText(PptWritingRequest request, HttpServletResponse response) {
        AiWritingFeignRequest aiRequest = new AiWritingFeignRequest();
        aiRequest.setModelId(request.getModelId());
        aiRequest.setInstruction(PptPromptBuilder.buildWritingPrompt(request.getContent(), request.getCommand()));
        doStreamPlainTextToResponse(aiRequest, response);
    }

    /**
     * 流式生成幻灯片 JSONL
     */
    @Override
    public void streamSlides(PptGenerateRequest request, HttpServletResponse response) {
        AiWritingFeignRequest aiRequest = new AiWritingFeignRequest();
        aiRequest.setModelId(request.getModelId());
        aiRequest.setInstruction(PptPromptBuilder.buildSlidesPrompt(request.getContent(), request.getLanguage(), request.getStyle()));
        doStreamJsonlToResponse(aiRequest, response);
    }

    /**
     * 执行纯文本流式转发
     */
    private void doStreamPlainTextToResponse(AiWritingFeignRequest request, HttpServletResponse response) {
        // 设置响应头
        response.setContentType("text/event-stream");// 设置响应类型为 SSE
        response.setCharacterEncoding("UTF-8");// 设置响应编码为 UTF-8
        response.setHeader("Cache-Control", "no-cache");// 禁用缓存
        response.setHeader("X-Accel-Buffering", "no");// 禁用网关和 Nginx 的缓冲
        
        try (OutputStream out = response.getOutputStream()) {// 创建输出流，用于将 SSE 响应数据写入响应体，给客户端提示进度信息
            sendSseMessage(out, SseMessage.status("正在连接 AI 创作中心..."));
            sendSseMessage(out, SseMessage.status("分析创作意图并构建 Prompt..."));

            restTemplate.execute(buildWritingUrl(), HttpMethod.POST, clientHttpRequest -> {
                clientHttpRequest.getHeaders().setContentType(MediaType.APPLICATION_JSON);// 设置请求头
                clientHttpRequest.getHeaders().setAccept(MediaType.parseMediaTypes(MediaType.TEXT_EVENT_STREAM_VALUE));// 设置响应类型为 SSE
                clientHttpRequest.getBody().write(toJsonBytes(request));// 将请求参数写入请求体
            }, (ResponseExtractor<Void>) httpResponse -> {
                sendSseMessage(out, SseMessage.status("AI 模型已响应，开始流式生成内容..."));
                streamAsSseMessage(httpResponse, out); // 将 SSE 响应流转换为 SseMessage
                sendSseMessage(out, SseMessage.status("生成完成！"));
                return null;
            });
            out.flush();
        } catch (Exception e) {
            log.error("流式转发纯文本失败", e);
        }
    }

    /**
     * 将 SSE 响应包装为 SseMessage 格式输出
     */
    private void streamAsSseMessage(ClientHttpResponse response, OutputStream out) throws IOException {
        InputStream in = response.getBody();
        byte[] buffer = new byte[1024];// 缓冲区大小
        int len;
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();// 存储一行数据
        List<String> eventData = new ArrayList<>(); // 存储事件数据
        
        while ((len = in.read(buffer)) != -1) {// 读取数据
            for (int i = 0; i < len; i++) {
                byte b = buffer[i];
                if (b == '\n') { // 遇到换行符
                    String line = lineBuffer.toString(StandardCharsets.UTF_8);
                    lineBuffer.reset();
                    
                    if (line.startsWith("data:")) {
                        eventData.add(line.substring(5));
                    } else if (line.isEmpty() && !eventData.isEmpty()) {
                        String joined = String.join("\n", eventData);
                        sendSseMessage(out, SseMessage.content(joined));
                        eventData.clear();
                    }
                } else if (b != '\r') {
                    lineBuffer.write(b);
                }
            }
        }
    }

    /**
     * 执行 JSONL 流式转发
     */
    private void doStreamJsonlToResponse(AiWritingFeignRequest request, HttpServletResponse response) {
        response.setContentType("text/event-stream; charset=utf-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        try (OutputStream out = response.getOutputStream()) {
            sendSseMessage(out, SseMessage.status("正在规划幻灯片页数与内容结构..."));
            sendSseMessage(out, SseMessage.status("正在根据风格要求进行排版设计..."));

            restTemplate.execute(buildWritingUrl(), HttpMethod.POST, clientHttpRequest -> {
                clientHttpRequest.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                clientHttpRequest.getHeaders().setAccept(MediaType.parseMediaTypes(MediaType.TEXT_EVENT_STREAM_VALUE));
                clientHttpRequest.getBody().write(toJsonBytes(request));
            }, (ResponseExtractor<Void>) httpResponse -> {
                sendSseMessage(out, SseMessage.status("开始构建幻灯片元素 JSON..."));
                streamJsonlToOutputStream(httpResponse, out);
                sendSseMessage(out, SseMessage.status("幻灯片结构构建完成！"));
                return null;
            });
            out.flush();
        } catch (Exception e) {
            log.error("流式转发幻灯片失败", e);
        }
    }

    /**
     * 流式处理 JSONL 响应并混入 SseMessage 格式
     */
    private void streamJsonlToOutputStream(ClientHttpResponse response, OutputStream out) throws IOException {
        StringBuilder buffer = new StringBuilder();
        boolean hasEnd = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    String chunk = line.substring(5);
                    if ("[DONE]".equals(chunk.trim())) {
                        continue;
                    }
                    buffer.append(chunk);
                    hasEnd = flushCompleteJsonObjects(buffer, out, hasEnd);
                }
            }
        }

        if (!buffer.isEmpty()) {
            hasEnd = flushCompleteJsonObjects(buffer, out, hasEnd);
        }

        if (!hasEnd) {
            sendSseMessage(out, SseMessage.content("{\"type\": \"end\"}"));
        }
    }

    private boolean flushCompleteJsonObjects(StringBuilder buffer, OutputStream out, boolean hasEnd) throws IOException {
        while (true) {
            int start = -1;
            for (int i = 0; i < buffer.length(); i++) {
                if (buffer.charAt(i) == '{') {
                    start = i;
                    break;
                }
            }
            if (start < 0) {
                buffer.setLength(0);
                break;
            }

            int depth = 0;
            boolean inString = false;
            boolean escape = false;
            int end = -1;

            for (int i = start; i < buffer.length(); i++) {
                char c = buffer.charAt(i);
                if (escape) {
                    escape = false;
                    continue;
                }
                if (c == '\\' && inString) {
                    escape = true;
                    continue;
                }
                if (c == '"') {
                    inString = !inString;
                    continue;
                }
                if (!inString) {
                    if (c == '{') depth++;
                    else if (c == '}') {
                        depth--;
                        if (depth == 0) {
                            end = i;
                            break;
                        }
                    }
                }
            }

            if (end < 0) break;

            String jsonStr = buffer.substring(start, end + 1).trim();
            buffer.delete(0, end + 1);

            try {
                Object node = objectMapper.readTree(jsonStr);
                String compact = objectMapper.writeValueAsString(node);
                if (compact.contains("\"type\":\"end\"") || compact.contains("\"type\": \"end\"")) {
                    hasEnd = true;
                }
                // 转发原始内容（因为它已经是 JSON）
                out.write((compact + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (Exception ignored) {}
        }
        return hasEnd;
    }

    /**
     * 核心方法：发送 SSE 消息并自动序列化为 JSON
     */
    private void sendSseMessage(OutputStream out, SseMessage msg) throws IOException {
        String json = objectMapper.writeValueAsString(msg);
        out.write((json + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private byte[] toJsonBytes(AiWritingFeignRequest request) {
        String json = "{"
                + (request.getModelId() != null ? "\"modelId\":" + request.getModelId() + "," : "")
                + "\"content\":" + quote(request.getContent()) + ","
                + "\"type\":" + quote(request.getType()) + ","
                + "\"instruction\":" + quote(request.getInstruction())
                + "}";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private String quote(String value) {
        String escaped = Objects.toString(value, "")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "\"" + escaped + "\"";
    }

    /**
     *  构建 API URL
     * @return API URL
     */
    private String buildWritingUrl() {
        return pptAiProperties.getBaseUrl().replaceAll("/+$", "") + "/ai/writing";
    }
}
