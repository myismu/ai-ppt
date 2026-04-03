package com.exambaby.ppt.service.service.impl;

import com.exambaby.ppt.service.config.PptAiProperties;
import com.exambaby.ppt.service.dto.request.PptGenerateRequest;
import com.exambaby.ppt.service.dto.request.PptOutlineRequest;
import com.exambaby.ppt.service.dto.request.PptWritingRequest;
import com.exambaby.ppt.service.feign.dto.AiWritingFeignRequest;
import com.exambaby.ppt.service.service.PptStreamService;
import com.exambaby.ppt.service.support.PptPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * PPT 文本流式转发实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PptStreamServiceImpl implements PptStreamService {

    private final RestTemplate restTemplate;
    private final PptAiProperties pptAiProperties;

    @Qualifier("pptStreamExecutor")
    private final Executor executor;

    @Override
    public SseEmitter streamOutline(PptOutlineRequest request) {
        AiWritingFeignRequest aiRequest = new AiWritingFeignRequest();
        aiRequest.setType("polish");
        aiRequest.setInstruction(PptPromptBuilder.buildOutlinePrompt(request.getContent(), request.getLanguage()));
        aiRequest.setContent(request.getContent());
        return streamPlainText(aiRequest);
    }

    @Override
    public SseEmitter streamSlides(PptGenerateRequest request) {
        AiWritingFeignRequest aiRequest = new AiWritingFeignRequest();
        aiRequest.setType("expand");
        aiRequest.setInstruction(PptPromptBuilder.buildSlidesPrompt(request.getContent(), request.getLanguage(), request.getStyle()));
        aiRequest.setContent(request.getContent());
        return streamJsonl(aiRequest);
    }

    @Override
    public SseEmitter streamWriting(PptWritingRequest request) {
        AiWritingFeignRequest aiRequest = new AiWritingFeignRequest();
        aiRequest.setType(resolveWritingType(request.getCommand()));
        aiRequest.setInstruction(PptPromptBuilder.buildWritingPrompt(request.getContent(), request.getCommand()));
        aiRequest.setContent(request.getContent());
        return streamPlainText(aiRequest);
    }

    private SseEmitter streamPlainText(AiWritingFeignRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        executor.execute(() -> doStreamPlainText(request, emitter));
        return emitter;
    }

    private SseEmitter streamJsonl(AiWritingFeignRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        executor.execute(() -> doStreamJsonl(request, emitter));
        return emitter;
    }

    private void doStreamPlainText(AiWritingFeignRequest request, SseEmitter emitter) {
        try {
            restTemplate.execute(buildWritingUrl(), HttpMethod.POST, clientHttpRequest -> {
                clientHttpRequest.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                clientHttpRequest.getHeaders().setAccept(MediaType.parseMediaTypes(MediaType.TEXT_EVENT_STREAM_VALUE));
                clientHttpRequest.getHeaders().setCacheControl("no-cache");
                clientHttpRequest.getBody().write(toJsonBytes(request));
            }, (ResponseExtractor<Void>) response -> {
                streamResponse(response, emitter);
                return null;
            });
            emitter.complete();
        } catch (Exception e) {
            log.error("流式转发大纲/文案失败", e);
            emitter.completeWithError(e);
        }
    }

    private void doStreamJsonl(AiWritingFeignRequest request, SseEmitter emitter) {
        try {
            restTemplate.execute(buildWritingUrl(), HttpMethod.POST, clientHttpRequest -> {
                clientHttpRequest.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                clientHttpRequest.getHeaders().setAccept(MediaType.parseMediaTypes(MediaType.TEXT_EVENT_STREAM_VALUE));
                clientHttpRequest.getHeaders().setCacheControl("no-cache");
                clientHttpRequest.getBody().write(toJsonBytes(request));
            }, (ResponseExtractor<Void>) response -> {
                streamJsonlResponse(response, emitter);
                return null;
            });
            emitter.complete();
        } catch (Exception e) {
            log.error("流式转发幻灯片失败", e);
            emitter.completeWithError(e);
        }
    }

    private void streamResponse(ClientHttpResponse response, SseEmitter emitter) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            char[] buffer = new char[1024];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                if (length > 0) {
                    emitter.send(SseEmitter.event().data(new String(buffer, 0, length)));
                }
            }
        }
    }

    private void streamJsonlResponse(ClientHttpResponse response, SseEmitter emitter) throws IOException {
        StringBuilder lineBuffer = new StringBuilder();
        boolean hasEnd = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            char[] buffer = new char[1024];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                if (length <= 0) {
                    continue;
                }
                lineBuffer.append(buffer, 0, length);
                int newlineIndex;
                while ((newlineIndex = indexOfNewline(lineBuffer)) >= 0) {
                    String rawLine = lineBuffer.substring(0, newlineIndex);
                    lineBuffer.delete(0, newlineIndex + 1);
                    String cleanLine = sanitizeJsonlLine(rawLine);
                    if (cleanLine == null) {
                        continue;
                    }
                    if (isEndLine(cleanLine)) {
                        hasEnd = true;
                    }
                    emitter.send(SseEmitter.event().data(cleanLine + "\n"));
                }
            }
        }

        String remaining = sanitizeJsonlLine(lineBuffer.toString());
        if (remaining != null) {
            if (isEndLine(remaining)) {
                hasEnd = true;
            }
            emitter.send(SseEmitter.event().data(remaining + "\n"));
        }

        if (!hasEnd) {
            emitter.send(SseEmitter.event().data("{\"type\": \"end\"}\n"));
        }
    }

    private int indexOfNewline(StringBuilder builder) {
        for (int i = 0; i < builder.length(); i++) {
            char c = builder.charAt(i);
            if (c == '\n') {
                return i;
            }
        }
        return -1;
    }

    private String sanitizeJsonlLine(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.replace("\r", "")
                .replace("```jsonl", "")
                .replace("```json", "")
                .replace("```", "")
                .trim();
        if (trimmed.isEmpty() || !trimmed.startsWith("{")) {
            return null;
        }
        if (!(trimmed.contains("\"type\": \"cover\"")
                || trimmed.contains("\"type\":\"cover\"")
                || trimmed.contains("\"type\": \"contents\"")
                || trimmed.contains("\"type\":\"contents\"")
                || trimmed.contains("\"type\": \"transition\"")
                || trimmed.contains("\"type\":\"transition\"")
                || trimmed.contains("\"type\": \"content\"")
                || trimmed.contains("\"type\":\"content\"")
                || trimmed.contains("\"type\": \"end\"")
                || trimmed.contains("\"type\":\"end\""))) {
            return null;
        }
        return trimmed;
    }

    private boolean isEndLine(String line) {
        return line.contains("\"type\": \"end\"") || line.contains("\"type\":\"end\"");
    }

    private String resolveWritingType(String command) {
        if (command == null || command.isBlank()) {
            return "polish";
        }
        return switch (command) {
            case "translate", "summarize", "expand" -> command;
            default -> "polish";
        };
    }

    private byte[] toJsonBytes(AiWritingFeignRequest request) {
        String json = "{"
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

    private String buildWritingUrl() {
        return pptAiProperties.getBaseUrl().replaceAll("/+$", "") + "/ai/writing";
    }
}
