package com.exambaby.ppt.service.service;

import com.exambaby.ppt.service.dto.request.PptImageRequest;

public interface PptGenerationService {

    String generateImage(PptImageRequest request);
}
