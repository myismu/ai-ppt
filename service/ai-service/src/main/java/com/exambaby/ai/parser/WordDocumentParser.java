package com.exambaby.ai.parser;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;

/**
 * Word (docx) 文档解析器
 */
@Component
public class WordDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String extension) {
        return "doc".equalsIgnoreCase(extension) || "docx".equalsIgnoreCase(extension);
    }

    @Override
    public String parse(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
