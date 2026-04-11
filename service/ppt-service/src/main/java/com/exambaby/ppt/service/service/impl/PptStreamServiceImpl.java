package com.exambaby.ppt.service.service.impl;

import com.exambaby.ppt.service.config.PptAiProperties;
import com.exambaby.ppt.service.dto.request.PptGenerateRequest;
import com.exambaby.ppt.service.dto.request.PptOutlineRequest;
import com.exambaby.ppt.service.dto.request.PptWritingRequest;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * PPT 流式服务实现类
 * 负责将 AI 服务返回的 SSE 流转换为前端可识别的格式
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
     * 将大纲请求转换为纯文本流式响应，前端可实时显示
     * 
     * @param request 大纲生成请求，包含主题、语言等信息
     * @param response HTTP 响应对象，用于写入流式数据
     */
    @Override
    public void streamPlainText(PptOutlineRequest request, HttpServletResponse response) {
        AiWritingFeignRequest aiRequest = new AiWritingFeignRequest();
        aiRequest.setInstruction(PptPromptBuilder.buildOutlinePrompt(request.getContent(), request.getLanguage()));
        doStreamPlainTextToResponse(aiRequest, response);
    }

    /**
     * 流式处理文案
     * 支持润色、扩写、翻译、摘要等操作，返回纯文本流
     * 
     * @param request 文案处理请求，包含内容和处理命令
     * @param response HTTP 响应对象，用于写入流式数据
     */
    @Override
    public void streamWritingText(PptWritingRequest request, HttpServletResponse response) {
        AiWritingFeignRequest aiRequest = new AiWritingFeignRequest();
        aiRequest.setInstruction(PptPromptBuilder.buildWritingPrompt(request.getContent(), request.getCommand()));
        doStreamPlainTextToResponse(aiRequest, response);
    }

    /**
     * 流式生成幻灯片 JSONL
     * 根据大纲生成结构化的幻灯片数据，以纯 JSONL 格式返回（每行一个 JSON 对象）
     * 
     * @param request 幻灯片生成请求，包含大纲、语言、风格等信息
     * @param response HTTP 响应对象
     */
    @Override
    public void streamSlides(PptGenerateRequest request, HttpServletResponse response) {
        AiWritingFeignRequest aiRequest = new AiWritingFeignRequest();
        aiRequest.setInstruction(PptPromptBuilder.buildSlidesPrompt(request.getContent(), request.getLanguage(), request.getStyle()));
        doStreamJsonlToResponse(aiRequest, response);
    }
    /**
     * 执行纯文本流式转发
     * 将 AI 服务的 SSE 响应解析为纯文本并实时推送给前端
     * 
     * @param request AI 写作请求
     * @param response HTTP 响应对象
     */
    private void doStreamPlainTextToResponse(AiWritingFeignRequest request, HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        
        try (OutputStream out = response.getOutputStream()) {
            restTemplate.execute(buildWritingUrl(), HttpMethod.POST, clientHttpRequest -> {
                clientHttpRequest.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                clientHttpRequest.getHeaders().setAccept(MediaType.parseMediaTypes(MediaType.TEXT_EVENT_STREAM_VALUE));
                clientHttpRequest.getHeaders().setCacheControl("no-cache");
                clientHttpRequest.getBody().write(toJsonBytes(request));
            }, (ResponseExtractor<Void>) httpResponse -> {
                streamAsPlainText(httpResponse, out);
                return null;
            });
            out.flush();
        } catch (Exception e) {
            log.error("流式转发纯文本失败", e);
        }
    }

    /**
     * 将 SSE 响应转换为纯文本流
     * 解析 SSE 格式（data: xxx），提取实际内容并实时刷新输出
     * 
     * SSE 格式说明：
     * - 每个 SSE 事件由多行 "data: xxx" 组成
     * - 空行表示事件结束
     * - 同一事件内的多个 data 行需要用换行符拼接
     * 
     * @param response AI 服务的 HTTP 响应
     * @param out 输出流，用于写入纯文本
     */
    private void streamAsPlainText(ClientHttpResponse response, OutputStream out) throws IOException {
        java.io.InputStream in = response.getBody();
        byte[] buffer = new byte[512];
        int len;
        java.io.ByteArrayOutputStream lineBuffer = new java.io.ByteArrayOutputStream();
        java.util.List<String> eventData = new java.util.ArrayList<>();
        
        while ((len = in.read(buffer)) != -1) {
            for (int i = 0; i < len; i++) {
                byte b = buffer[i];
                if (b == '\n') {
                    String line = lineBuffer.toString(StandardCharsets.UTF_8);
                    lineBuffer.reset();
                    
                    if (line.startsWith("data:")) {
                        eventData.add(line.substring(5));
                    } else if (line.isEmpty() && !eventData.isEmpty()) {
                        String joined = String.join("\n", eventData);
                        out.write(joined.getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        eventData.clear();
                    }
                } else if (b != '\r') {
                    lineBuffer.write(b);
                }
            }
        }
        
        if (lineBuffer.size() > 0) {
            String line = lineBuffer.toString(StandardCharsets.UTF_8);
            if (line.startsWith("data:")) {
                eventData.add(line.substring(5));
            }
        }
        
        if (!eventData.isEmpty()) {
            String joined = String.join("\n", eventData);
            out.write(joined.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    /**
     * 执行 JSONL 流式转发
     * 将 AI 服务的 SSE 响应解析为纯 JSONL 并实时推送给前端
     * 
     * @param request AI 写作请求
     * @param response HTTP 响应对象
     */
    private void doStreamJsonlToResponse(AiWritingFeignRequest request, HttpServletResponse response) {
        // Content-Type 必须是 text/event-stream，前端 fetch.ts 根据此值判断是否为流式响应
        response.setContentType("text/event-stream; charset=utf-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        try (OutputStream out = response.getOutputStream()) {
            restTemplate.execute(buildWritingUrl(), HttpMethod.POST, clientHttpRequest -> {
                clientHttpRequest.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                clientHttpRequest.getHeaders().setAccept(MediaType.parseMediaTypes(MediaType.TEXT_EVENT_STREAM_VALUE));
                clientHttpRequest.getHeaders().setCacheControl("no-cache");
                clientHttpRequest.getBody().write(toJsonBytes(request));
            }, (ResponseExtractor<Void>) httpResponse -> {
                streamJsonlToOutputStream(httpResponse, out);
                return null;
            });
            out.flush();
        } catch (Exception e) {
            log.error("流式转发幻灯片失败", e);
        }
    }

    /**
     * 流式处理 JSONL 响应
     * 解析 AI 返回的 SSE 响应，将每个完整的 JSON 对象作为独立的一行输出
     * 
     * 上游 AI 服务逐 token 流式输出，每个 SSE 事件只包含几个字符的片段，例如：
     *   data:{"type
     *   data:": "cover
     *   data:", "data":
     *   data: {"title": "xxx"}}
     * 
     * 所有片段拼接后构成完整的 JSONL：
     *   {"type": "cover", "data": {"title": "xxx"}}
     *   {"type": "contents", "data": {...}}
     * 
     * 本方法持续累积所有片段，通过花括号平衡计数检测完整 JSON 对象，
     * 一旦检测到完整对象就立即输出一行。
     * 
     * @param response AI 服务的 HTTP 响应
     * @param out 输出流
     */
    private void streamJsonlToOutputStream(ClientHttpResponse response, OutputStream out) throws IOException {
        StringBuilder buffer = new StringBuilder();
        boolean hasEnd = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    String chunk = line.substring(5);
                    // 跳过 SSE 结束标记
                    if ("[DONE]".equals(chunk.trim())) {
                        continue;
                    }
                    buffer.append(chunk);

                    // 每次追加片段后，尝试从 buffer 中提取完整的 JSON 对象
                    hasEnd = flushCompleteJsonObjects(buffer, out, hasEnd);
                }
                // 空行和非 data: 行直接忽略
            }
        }

        // 处理 buffer 中剩余内容（如果有未输出的完整 JSON）
        if (!buffer.isEmpty()) {
            hasEnd = flushCompleteJsonObjects(buffer, out, hasEnd);
        }

        // 最后检查 buffer 里是否还有可解析的内容
        String remaining = buffer.toString().trim();
        if (!remaining.isEmpty()) {
            String compact = compactJson(remaining);
            if (compact != null) {
                if (isEndLine(compact)) {
                    hasEnd = true;
                }
                out.write((compact + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        }

        // 如果上游没有发送 end 标记，补充一个
        if (!hasEnd) {
            out.write("{\"type\": \"end\"}\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    /**
     * 从 buffer 中提取并输出所有完整的 JSON 对象
     * 使用花括号平衡计数法检测 JSON 对象边界，正确处理字符串内的转义字符
     * 
     * @param buffer 累积的文本缓冲区，已输出的部分会被移除
     * @param out 输出流
     * @param hasEnd 是否已经遇到 end 标记
     * @return 更新后的 hasEnd 值
     */
    private boolean flushCompleteJsonObjects(StringBuilder buffer, OutputStream out, boolean hasEnd) throws IOException {
        while (true) {
            // 找到第一个 '{' 作为 JSON 对象起点
            int start = -1;
            for (int i = 0; i < buffer.length(); i++) {
                if (buffer.charAt(i) == '{') {
                    start = i;
                    break;
                }
            }
            if (start < 0) {
                // 没有 JSON 对象起点，清空无用前缀
                buffer.setLength(0);
                break;
            }

            // 花括号计数法：找到匹配的 '}' 作为对象终点
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
                    if (c == '{') {
                        depth++;
                    } else if (c == '}') {
                        depth--;
                        if (depth == 0) {
                            end = i;
                            break;
                        }
                    }
                }
            }

            if (end < 0) {
                // JSON 对象还不完整，等待更多数据
                break;
            }

            // 提取完整的 JSON 字符串
            String jsonStr = buffer.substring(start, end + 1).trim();
            // 从 buffer 中移除已处理的部分
            buffer.delete(0, end + 1);

            // 压缩为单行 JSON 并输出
            String compact = compactJson(jsonStr);
            if (compact != null) {
                if (isEndLine(compact)) {
                    hasEnd = true;
                }
                out.write((compact + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        }
        return hasEnd;
    }

    /**
     * 将 JSON 字符串压缩为紧凑的单行格式
     * 解析后重新序列化，去除换行和缩进
     * 
     * @param json 原始 JSON 字符串（可能是多行格式化的）
     * @return 紧凑的单行 JSON 字符串，解析失败返回 null
     */
    private String compactJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            Object node = objectMapper.readTree(json);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证是否为有效的 JSON
     */
    private boolean isValidJson(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            objectMapper.readTree(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否为结束行
     * 
     * @param line JSON 行
     * @return 是否为结束标记
     */
    private boolean isEndLine(String line) {
        return line.contains("\"type\": \"end\"") || line.contains("\"type\":\"end\"");
    }

    /**
     * 解析文案处理类型
     * 将前端命令映射为 AI 服务可识别的类型
     * 
     * @param command 前端命令
     * @return AI 服务类型
     */
    private String resolveWritingType(String command) {
        if (command == null || command.isBlank()) {
            return "polish";
        }
        return switch (command) {
            case "translate", "summarize", "expand" -> command;
            default -> "polish";
        };
    }

    /**
     * 将请求对象序列化为 JSON 字节数组
     * 
     * @param request AI 写作请求
     * @return JSON 字节数组
     */
    private byte[] toJsonBytes(AiWritingFeignRequest request) {
        String json = "{"
                + "\"content\":" + quote(request.getContent()) + ","
                + "\"type\":" + quote(request.getType()) + ","
                + "\"instruction\":" + quote(request.getInstruction())
                + "}";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 将字符串转换为 JSON 引号格式
     * 处理转义字符，确保 JSON 格式正确
     * 
     * @param value 原始字符串
     * @return 带引号的 JSON 字符串
     */
    private String quote(String value) {
        String escaped = Objects.toString(value, "")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "\"" + escaped + "\"";
    }

    /**
     * 构建 AI 写作服务的完整 URL
     * 
     * @return AI 服务 URL
     */
    private String buildWritingUrl() {
        return pptAiProperties.getBaseUrl().replaceAll("/+$", "") + "/ai/writing";
    }
}
