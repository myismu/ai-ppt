package com.exambaby.ppt.service.service;

import com.exambaby.ppt.service.dto.request.PptGenerateRequest;
import com.exambaby.ppt.service.dto.request.PptOutlineRequest;
import com.exambaby.ppt.service.dto.request.PptWritingRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface PptStreamService {

    SseEmitter streamOutline(PptOutlineRequest request);

    SseEmitter streamSlides(PptGenerateRequest request);

    SseEmitter streamWriting(PptWritingRequest request);
}
