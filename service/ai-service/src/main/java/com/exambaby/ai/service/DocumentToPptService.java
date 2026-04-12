package com.exambaby.ai.service;

import com.exambaby.ai.adapter.AiTextAdapter;
import com.exambaby.ai.dto.AiWritingRequest;
import com.exambaby.ai.dto.DocumentToPptRequest;
import com.exambaby.ai.parser.DocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.List;

/**
 * 文档转 PPT 大纲服务
 */
@Slf4j
@Service
public class DocumentToPptService {

    private final List<DocumentParser> documentParsers;
    private final AiTextAdapter aiTextAdapter;

    // 文件上传的基础路径（本地文件服务存放路径）
    private static final String UPLOAD_BASE_PATH = "E:\\demo\\question-bank-cloud\\service\\file-service\\upload\\projects";

    public DocumentToPptService(List<DocumentParser> documentParsers, AiTextAdapter aiTextAdapter) {
        this.documentParsers = documentParsers;
        this.aiTextAdapter = aiTextAdapter;
    }

    /**
     * 解析文档并调用大模型生成 PPT 大纲 (流式返回)
     * @param request 请求对象
     * @return 增量文本流
     */
    public Flux<String> generatePptOutlineStream(DocumentToPptRequest request) {
        return Mono.fromCallable(() -> parseDocumentContent(request.getFileUrl()))
                .flatMapMany(documentText -> {
                    // 数据清洗与防爆破截断（限制前 10 万字符）
                    String cleanText = preprocessText(documentText);

                    // 组装大模型所需的请求对象
                    AiWritingRequest aiRequest = new AiWritingRequest();
                    aiRequest.setModelId(request.getModelId());
                    aiRequest.setType("document_to_ppt"); 
                    
                    // 将用户指令和文档内容拼接
                    String instruction = request.getPrompt() != null ? request.getPrompt() : "请根据提供的文档生成PPT";
                    aiRequest.setInstruction("【用户具体需求】\n" + instruction);
                    aiRequest.setContent("【参考资料】\n" + cleanText);
                    
                    log.info("开始调用 AI 适配器进行文档转 PPT 大纲生成...");
                    
                    Flux<String> statusStream = Flux.just(
                        "{\"type\":\"status\",\"content\":\"文档解析完成，正在连接 AI 创作中心...\"}",
                        "{\"type\":\"status\",\"content\":\"分析参考资料并构建逻辑大纲...\"}",
                        "{\"type\":\"status\",\"content\":\"AI 模型已响应，开始流式生成内容...\"}"
                    );
                    
                    Flux<String> contentStream = aiTextAdapter.streamWriting(aiRequest)
                        .map(chunk -> "{\"type\":\"content\",\"content\":\"" + escapeJson(chunk) + "\"}");
                        
                    return Flux.concat(statusStream, contentStream);
                });
    }
    
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }

    /**
     * 辅助解析文档
     */
    private String parseDocumentContent(String fileUrl) throws Exception {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        
        // 尝试构建绝对路径。如果前端传的是相对路径或文件名，将其拼接到 base path 后
        String path = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
        String absolutePath = UPLOAD_BASE_PATH + File.separator + path.replace("/", File.separator);
        File file = new File(absolutePath);
        
        if (!file.exists()) {
            throw new RuntimeException("文件不存在: " + file.getAbsolutePath());
        }

        String extension = getExtension(file.getName());
        for (DocumentParser parser : documentParsers) {
            if (parser.supports(extension)) {
                log.info("使用 {} 解析文件: {}", parser.getClass().getSimpleName(), file.getAbsolutePath());
                return parser.parse(file);
            }
        }
        
        throw new UnsupportedOperationException("不支持的文件类型: " + extension);
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }

    private String preprocessText(String text) {
        if (text == null) return "";
        // 简单清洗：去除多余的空行
        text = text.replaceAll("(?m)^\\s*$\n", "");
        // 限制最大字数 10 万字，避免大模型 Token 溢出或 OOM
        int maxLength = 100000;
        if (text.length() > maxLength) {
            log.warn("文档内容过长，截断为 {} 字符", maxLength);
            text = text.substring(0, maxLength);
        }
        return text;
    }
}
